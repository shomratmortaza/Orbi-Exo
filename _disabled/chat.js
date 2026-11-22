// ORBI AI proxy (Google Gemini). Set GEMINI_API_KEY in Vercel > Settings > Environment Variables.
const hits=new Map();
module.exports=async(req,res)=>{
  if(req.method!=='POST')return res.status(405).json({error:'POST only'});
  const key=process.env.GEMINI_API_KEY;
  if(!key)return res.status(503).json({error:'AI key not configured on the server'});
  const ip=(req.headers['x-forwarded-for']||'x').split(',')[0],now=Date.now();
  const h=(hits.get(ip)||[]).filter(t=>now-t<60000);
  if(h.length>=15)return res.status(429).json({error:'Too many requests, wait a minute'});
  hits.set(ip,[...h,now]);
  const msgs=(req.body&&req.body.messages||[]).slice(-12).filter(m=>['user','assistant'].includes(m.role)&&typeof m.content==='string').map(m=>({role:m.role==='assistant'?'model':'user',parts:[{text:m.content.slice(0,2000)}]}));
  if(!msgs.length||msgs[msgs.length-1].role!=='user')return res.status(400).json({error:'Bad request'});
  const model=process.env.GEMINI_MODEL||'gemini-2.5-flash';
  try{
    const r=await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,{method:'POST',headers:{'content-type':'application/json','x-goog-api-key':key},
      body:JSON.stringify({systemInstruction:{parts:[{text:'You are ORBI AI, a concise assistant living in a productivity island. Keep answers short and practical.'}]},contents:msgs,generationConfig:{maxOutputTokens:1000}})});
    const j=await r.json();
    if(!r.ok)return res.status(502).json({error:(j.error&&j.error.message)||'AI request failed'});
    const text=((j.candidates&&j.candidates[0]&&j.candidates[0].content&&j.candidates[0].content.parts)||[]).map(p=>p.text||'').join('\n').trim();
    if(!text)return res.status(502).json({error:'The AI returned no answer. Try rephrasing.'});
    res.json({text});
  }catch(e){res.status(502).json({error:'AI service unreachable'})}
};
