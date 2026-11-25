const {app,BrowserWindow,Tray,Menu,globalShortcut,ipcMain,screen,nativeImage,shell,net}=require('electron');
const {execFile}=require('child_process');
const path=require('path'),fs=require('fs');
app.commandLine.appendSwitch('autoplay-policy','no-user-gesture-required');
const URL_='https://orbi-exo.vercel.app/',W=720,H=540,KEY='CommandOrControl+Alt+O';
let win,tray,cfg={display:null};const cf=()=>path.join(app.getPath('userData'),'orbi-desktop.json');
const load=()=>{try{cfg={...cfg,...JSON.parse(fs.readFileSync(cf(),'utf8'))}}catch{}},save=()=>{try{fs.writeFileSync(cf(),JSON.stringify(cfg))}catch{}};
if(!app.requestSingleInstanceLock())app.quit();
const disp=()=>screen.getAllDisplays().find(d=>d.id===cfg.display)||screen.getPrimaryDisplay();
function place(){if(!win)return;const b=disp().bounds;let x=Math.round(b.x+(b.width-W)/2),y=b.y;
 if(cfg.pos){const px=cfg.pos.x,py=cfg.pos.y;const ok=screen.getAllDisplays().some(d=>{const r=d.bounds;return px+W/2>=r.x&&px+W/2<=r.x+r.width&&py>=r.y-5&&py<=r.y+r.height-40});if(ok){x=px;y=py}}
 win.setBounds({x:Math.round(x),y:Math.round(y),width:W,height:H})}
function create(){
 win=new BrowserWindow({width:W,height:H,frame:false,transparent:true,resizable:false,movable:false,minimizable:false,maximizable:false,skipTaskbar:true,alwaysOnTop:true,hasShadow:false,show:false,backgroundColor:'#00000000',webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true}});
 place();win.setIgnoreMouseEvents(true,{forward:true});win.loadURL(URL_);
 win.webContents.on('did-fail-load',(e,c,d,u,main)=>{if(main)win.loadFile(path.join(__dirname,'offline.html'))});
 win.webContents.setWindowOpenHandler(({url})=>{shell.openExternal(url);return{action:'deny'}});
 win.webContents.on('will-navigate',(e,u)=>{if(!u.startsWith(URL_)&&!u.startsWith('file:')){e.preventDefault();shell.openExternal(u)}});
 win.webContents.on('did-finish-load',()=>win.webContents.send('dock',cfg.dock||null));
 win.once('ready-to-show',()=>win.showInactive())}
function toggleIsland(){if(!win)return;win.show();win.setIgnoreMouseEvents(false);win.focus();win.webContents.send('toggle')}
function menu(){const ds=screen.getAllDisplays();return Menu.buildFromTemplate([
 {label:'Open / close island',click:toggleIsland},
 {label:win&&win.isVisible()?'Hide ORBI':'Show ORBI',click:()=>{win.isVisible()?win.hide():win.showInactive();tray.setContextMenu(menu())}},
 {label:'Reset island position',click:()=>{cfg.pos=null;cfg.dock=null;save();place();win&&win.webContents.send('dock',null);win&&win.webContents.reload()}},
 {label:'Show on display',submenu:ds.map((d,i)=>({label:`Display ${i+1} (${d.bounds.width}×${d.bounds.height})`,type:'radio',checked:disp().id===d.id,click:()=>{cfg.display=d.id;save();place()}}))},
 {label:'Start with Windows',type:'checkbox',checked:app.getLoginItemSettings().openAtLogin,click:i=>app.setLoginItemSettings({openAtLogin:i.checked})},
 {type:'separator'},{label:'Quit ORBI',click:()=>app.quit()}])}
app.whenReady().then(()=>{load();create();
 tray=new Tray(nativeImage.createFromPath(path.join(__dirname,'assets','tray.png')));tray.setToolTip('ORBI — '+KEY+' to open');tray.setContextMenu(menu());tray.on('click',toggleIsland);
 globalShortcut.register(KEY,toggleIsland);
 ipcMain.on('ignore',(e,v)=>{if(win)win.setIgnoreMouseEvents(v,{forward:true})});

 // ---- system media (Windows SMTC: Spotify, YouTube Music, browsers...) ----
 const ps=s=>new Promise(r=>execFile('powershell.exe',['-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-EncodedCommand',Buffer.from(s,'utf16le').toString('base64')],{windowsHide:true,timeout:8000},(e,o)=>r(e?'':String(o).trim())));
 const smtc=c=>["Add-Type -AssemblyName System.Runtime.WindowsRuntime",
  "$at=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation\x601'})[0]",
  "function Aw($t,$r){$m=$at.MakeGenericMethod($r);$n=$m.Invoke($null,@($t));$n.Wait(-1)|Out-Null;$n.Result}",
  "[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]",
  "$mg=Aw ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
  "$s=$mg.GetCurrentSession()","if(-not $s){'{}';exit}","$cmd='"+c+"'",
  "if($cmd -eq 'toggle'){[void](Aw ($s.TryTogglePlayPauseAsync()) ([bool]))}elseif($cmd -eq 'next'){[void](Aw ($s.TrySkipNextAsync()) ([bool]))}elseif($cmd -eq 'prev'){[void](Aw ($s.TrySkipPreviousAsync()) ([bool]))}",
  "$p=Aw ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
  "@{ok=$true;granted=$true;title=$p.Title;artist=$p.Artist;playing=($s.GetPlaybackInfo().PlaybackStatus -eq 'Playing')}|ConvertTo-Json -Compress"].join("\n");
 ipcMain.handle('media-status',async()=>{try{return Object.assign({granted:true},JSON.parse(await ps(smtc('status'))||'{}'))}catch{return{granted:true}}});
 ipcMain.on('media-cmd',(e,c)=>{if(['toggle','next','prev'].includes(c))ps(smtc(c))});
 // ---- in-app update ----
 ipcMain.on('version',e=>{e.returnValue=app.getVersion()});
 ipcMain.handle('update',async()=>{try{const r=await net.fetch('https://github.com/shomratmortaza/Orbi-Exo/releases/latest/download/ORBI-Setup-x64.exe');if(!r.ok)return false;const f=path.join(app.getPath('temp'),'ORBI-Setup-x64.exe');fs.writeFileSync(f,Buffer.from(await r.arrayBuffer()));await shell.openPath(f);setTimeout(()=>app.quit(),800);return true}catch{return false}});
 let base=null,restoreY=null;const near=()=>{const b=win.getBounds();return screen.getDisplayNearestPoint({x:b.x+W/2,y:b.y+30}).bounds};
 ipcMain.on('dragstart',()=>{if(win){const b=win.getBounds();base={x:b.x,y:b.y}}});
 ipcMain.on('dragmove',(e,dx,dy,iw)=>{if(!win||!base)return;const g=(W-iw)/2;let x=base.x+dx,y=base.y+dy;const c=screen.getDisplayNearestPoint({x:Math.round(x+W/2),y:Math.round(y+30)}).bounds;x=Math.min(Math.max(x,c.x-g),c.x+c.width-g-iw);y=Math.min(Math.max(y,c.y),c.y+c.height-60);win.setPosition(Math.round(x),Math.round(y))});
 ipcMain.on('dragend',()=>{if(win){const b=win.getBounds();cfg.pos={x:b.x,y:b.y};cfg.dock=null;save()}});
 ipcMain.on('dock',(e,side)=>{if(!win)return;const c=near(),g=(W-22)/2,b=win.getBounds(),x=Math.round(side==='l'?c.x-g:c.x+c.width-g-22);win.setPosition(x,b.y);cfg.dock=side;cfg.pos={x,y:b.y};save()});
 ipcMain.on('undock',(e,iw)=>{if(!win)return;const c=near(),b=win.getBounds(),g=(W-iw)/2,x=Math.round(cfg.dock==='l'?c.x+10-g:c.x+c.width-10-iw-g);win.setPosition(x,b.y);cfg.dock=null;cfg.pos={x,y:b.y};save()});
 ipcMain.on('open',(e,o)=>{if(!win)return;const b=win.getBounds(),c=near();if(o){restoreY=b.y;const mx=c.y+c.height-H;if(b.y>mx)win.setPosition(b.x,Math.max(c.y,mx))}else if(restoreY!=null){win.setPosition(b.x,restoreY);restoreY=null}});

 ['display-metrics-changed','display-added','display-removed'].forEach(ev=>screen.on(ev,()=>{place();tray.setContextMenu(menu())}))});
app.on('second-instance',toggleIsland);
app.on('will-quit',()=>globalShortcut.unregisterAll());
app.on('window-all-closed',()=>app.quit());
