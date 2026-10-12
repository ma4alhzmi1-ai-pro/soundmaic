import http from "node:http";
import crypto from "node:crypto";

const PORT = Number(process.env.PORT || 3000);
const MAX_TEXT_BYTES = 20_000;
const MAX_AUDIO_BYTES = 15 * 1024 * 1024;
const REQUEST_TIMEOUT_MS = 45_000;
const RATE_WINDOW_MS = 60_000;
const RATE_LIMIT = 20;
const ELEVENLABS_API = "https://api.elevenlabs.io/v1";
const CLIENT_HEADER = "android";

const voiceProfiles = Object.freeze({
  young_woman: {
    label: "فتاة شابة",
    voiceId: "EXAVITQu4vr4xnSDxMaL"
  },
  middle_woman: {
    label: "امرأة ناضجة",
    voiceId: "21m00Tcm4TlvDq8ikWAM"
  },
  child_10: {
    label: "طفل",
    voiceId: "MF3mGyEYCl7XYWbV9V6O"
  },
  young_man: {
    label: "شاب",
    voiceId: "TxGEqnHWrfWFTfGW9XjX"
  },
  elderly_man: {
    label: "رجل مسن",
    voiceId: "pNInz6obpgDQGcFmaJgB"
  },
  robot: {
    label: "روبوت",
    voiceId: "ErXwobaYiN019PkySvjV"
  },
  alien: {
    label: "كائن فضائي",
    voiceId: "MF3mGyEYCl7XYWbV9V6O"
  },
  monster: {
    label: "وحش",
    voiceId: "pNInz6obpgDQGcFmaJgB"
  }
});

const rateBuckets = new Map();

function json(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff"
  });
  res.end(body);
}

function clientAddress(req) {
  const forwarded = req.headers["x-forwarded-for"];
  return (typeof forwarded === "string" ? forwarded.split(",")[0] : req.socket.remoteAddress) || "unknown";
}

function checkRateLimit(req) {
  const key = `${clientAddress(req)}:${req.headers["x-soundmaic-client"] || "unknown"}`;
  const now = Date.now();
  const bucket = rateBuckets.get(key) || { startedAt: now, count: 0 };
  if (now - bucket.startedAt >= RATE_WINDOW_MS) {
    bucket.startedAt = now;
    bucket.count = 0;
  }
  bucket.count += 1;
  rateBuckets.set(key, bucket);
  return bucket.count <= RATE_LIMIT;
}

function isAuthorized(req) {
  // The mobile client identifier is intentionally not treated as a secret.
  // The ElevenLabs key remains server-only; rate limiting and profile allowlisting
  // protect the public mobile endpoint from accidental or automated abuse.
  return req.headers["x-soundmaic-client"] === CLIENT_HEADER;
}

function readBody(req, maxBytes) {
  return new Promise((resolve, reject) => {
    let total = 0;
    const chunks = [];
    req.on("data", (chunk) => {
      total += chunk.length;
      if (total > maxBytes) {
        reject(Object.assign(new Error("payload too large"), { statusCode: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

async function callElevenLabs(path, options) {
  if (!process.env.ELEVENLABS_API_KEY) {
    throw Object.assign(new Error("provider unavailable"), { statusCode: 503 });
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const response = await fetch(`${ELEVENLABS_API}${path}`, {
      ...options,
      headers: {
        ...(options.headers || {}),
        "xi-api-key": process.env.ELEVENLABS_API_KEY,
        Accept: "audio/mpeg"
      },
      signal: controller.signal
    });
    if (!response.ok) {
      const providerStatus = response.status;
      throw Object.assign(new Error("voice provider request failed"), {
        statusCode: providerStatus === 429 ? 429 : 502
      });
    }
    return Buffer.from(await response.arrayBuffer());
  } finally {
    clearTimeout(timeout);
  }
}

function profileFromRequest(reqUrl) {
  const profile = reqUrl.searchParams.get("profile") || "young_woman";
  return voiceProfiles[profile] ? { id: profile, ...voiceProfiles[profile] } : null;
}

async function handleTextToSpeech(req, res, url) {
  const profile = profileFromRequest(url);
  if (!profile) return json(res, 400, { error: "الصوت المطلوب غير متاح" });
  const raw = await readBody(req, MAX_TEXT_BYTES);
  let input;
  try {
    input = JSON.parse(raw.toString("utf8"));
  } catch {
    return json(res, 400, { error: "صيغة الطلب غير صحيحة" });
  }
  const text = typeof input.text === "string" ? input.text.trim() : "";
  if (!text || text.length > 5000) {
    return json(res, 400, { error: "النص مطلوب وبحد أقصى 5000 حرف" });
  }
  const speed = Number.isFinite(Number(input.speed))
    ? Math.min(1.2, Math.max(0.8, Number(input.speed)))
    : 1.0;
  const audio = await callElevenLabs(`/text-to-speech/${encodeURIComponent(profile.voiceId)}?output_format=mp3_44100_128`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      text,
      model_id: "eleven_multilingual_v2",
      voice_settings: {
        stability: 0.52,
        similarity_boost: 0.78,
        style: 0.18,
        use_speaker_boost: true
      },
      pronunciation_dictionary_locators: [],
      speed
    })
  });
  res.writeHead(200, {
    "Content-Type": "audio/mpeg",
    "Content-Length": audio.length,
    "Cache-Control": "no-store",
    "Content-Disposition": "inline; filename=voice.mp3",
    "X-Content-Type-Options": "nosniff"
  });
  res.end(audio);
}

async function handleVoiceConversion(req, res, url) {
  const profile = profileFromRequest(url);
  if (!profile) return json(res, 400, { error: "الصوت المطلوب غير متاح" });
  const contentType = req.headers["content-type"];
  if (typeof contentType !== "string" || !contentType.startsWith("multipart/form-data;")) {
    return json(res, 400, { error: "يجب إرسال التسجيل بصيغة multipart/form-data" });
  }
  const raw = await readBody(req, MAX_AUDIO_BYTES);
  const audio = await callElevenLabs(`/speech-to-speech/${encodeURIComponent(profile.voiceId)}?output_format=mp3_44100_128`, {
    method: "POST",
    headers: { "Content-Type": contentType },
    body: raw
  });
  res.writeHead(200, {
    "Content-Type": "audio/mpeg",
    "Content-Length": audio.length,
    "Cache-Control": "no-store",
    "Content-Disposition": "inline; filename=converted-voice.mp3",
    "X-Content-Type-Options": "nosniff"
  });
  res.end(audio);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
  try {
    if (req.method === "GET" && url.pathname === "/health") {
      return json(res, 200, { ok: true, service: "soundmaic-voice-api" });
    }
    if (req.method === "GET" && url.pathname === "/api/voices") {
      return json(res, 200, Object.entries(voiceProfiles).map(([id, profile]) => ({ id, label: profile.label })));
    }
    if (!isAuthorized(req)) return json(res, 401, { error: "طلب غير مصرح" });
    if (!checkRateLimit(req)) return json(res, 429, { error: "تم تجاوز حد الطلبات، حاول بعد دقيقة" });
    if (req.method === "POST" && url.pathname === "/api/tts") {
      return await handleTextToSpeech(req, res, url);
    }
    if (req.method === "POST" && url.pathname === "/api/voice-convert") {
      return await handleVoiceConversion(req, res, url);
    }
    return json(res, 404, { error: "المسار غير موجود" });
  } catch (error) {
    const status = Number(error.statusCode) || (error.name === "AbortError" ? 504 : 500);
    console.error(`request_failed status=${status} request=${crypto.randomUUID()}`);
    if (!res.headersSent) json(res, status, { error: status === 429 ? "تم تجاوز الحد" : "تعذر إنشاء الصوت حاليًا" });
    else res.destroy();
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`soundmaic-voice-api listening on ${PORT}`);
});
