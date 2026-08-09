const RAILWAY = 'https://daycarelog-production-0a50.up.railway.app'

export default async function handler(req, res) {
  try {
    const target = RAILWAY + req.url

    const headers = { accept: 'application/json' }
    if (req.headers['content-type'])   headers['content-type']   = req.headers['content-type']
    if (req.headers['authorization'])  headers['authorization']  = req.headers['authorization']

    const opts = { method: req.method, headers }

    if (req.method !== 'GET' && req.method !== 'HEAD') {
      const chunks = []
      for await (const chunk of req) chunks.push(chunk)
      const raw = Buffer.concat(chunks).toString('utf-8')
      if (raw) opts.body = raw
    }

    const upstream = await fetch(target, opts)
    const text = await upstream.text()
    res.status(upstream.status)
    res.setHeader('Content-Type', 'application/json')
    try { res.json(JSON.parse(text)) } catch { res.end(text) }
  } catch (err) {
    res.status(502).json({ message: err.message })
  }
}

export const config = { api: { bodyParser: false } }
