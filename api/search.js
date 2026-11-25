// Vercel serverless function: song search for ORBI.
// The YouTube API key lives ONLY in Vercel's environment variables (YOUTUBE_API_KEY), never in the repo or the browser.
const hits = new Map();

const decode = (s) => String(s || '')
  .replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>');

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  const q = String((req.query && req.query.q) || '').trim().slice(0, 100);
  if (!q) return res.status(400).json({ error: 'Type a song name' });

  const key = process.env.YOUTUBE_API_KEY;
  if (!key) return res.status(503).json({ error: 'Search is not set up yet' });

  // very small per-visitor limit so one person cannot burn the daily quota
  const ip = String(req.headers['x-forwarded-for'] || 'x').split(',')[0].trim();
  const now = Date.now();
  const recent = (hits.get(ip) || []).filter((t) => now - t < 60000);
  if (recent.length >= 15) return res.status(429).json({ error: 'Too many searches, wait a minute' });
  recent.push(now);
  hits.set(ip, recent);
  if (hits.size > 5000) hits.clear();

  try {
    const url = 'https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoEmbeddable=true&maxResults=8&q='
      + encodeURIComponent(q) + '&key=' + encodeURIComponent(key);
    const r = await fetch(url);
    const j = await r.json();
    if (!r.ok) return res.status(r.status === 403 ? 429 : 502).json({ error: 'Search limit reached for today, try a link instead' });
    res.setHeader('Cache-Control', 'public, s-maxage=86400, stale-while-revalidate=86400');
    return res.status(200).json({
      items: (j.items || []).map((i) => ({ id: i.id.videoId, title: decode(i.snippet.title), channel: decode(i.snippet.channelTitle) })),
    });
  } catch (e) {
    return res.status(502).json({ error: 'Search failed' });
  }
};
