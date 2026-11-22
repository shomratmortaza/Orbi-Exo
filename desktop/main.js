const {app,BrowserWindow,Tray,Menu,globalShortcut,ipcMain,screen,nativeImage,shell}=require('electron');
const path=require('path'),fs=require('fs');
const URL_='https://orbi-exo.vercel.app/',W=720,H=540,KEY='CommandOrControl+Alt+O';
let win,tray,cfg={display:null};const cf=()=>path.join(app.getPath('userData'),'orbi-desktop.json');
const load=()=>{try{cfg={...cfg,...JSON.parse(fs.readFileSync(cf(),'utf8'))}}catch{}},save=()=>{try{fs.writeFileSync(cf(),JSON.stringify(cfg))}catch{}};
if(!app.requestSingleInstanceLock())app.quit();
const disp=()=>screen.getAllDisplays().find(d=>d.id===cfg.display)||screen.getPrimaryDisplay();
function place(){if(!win)return;const b=disp().bounds;win.setBounds({x:Math.round(b.x+(b.width-W)/2),y:b.y,width:W,height:H})}
function create(){
 win=new BrowserWindow({width:W,height:H,frame:false,transparent:true,resizable:false,movable:false,minimizable:false,maximizable:false,skipTaskbar:true,alwaysOnTop:true,hasShadow:false,show:false,backgroundColor:'#00000000',webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true}});
 place();win.setIgnoreMouseEvents(true,{forward:true});win.loadURL(URL_);
 win.webContents.on('did-fail-load',(e,c,d,u,main)=>{if(main)win.loadFile(path.join(__dirname,'offline.html'))});
 win.webContents.setWindowOpenHandler(({url})=>{shell.openExternal(url);return{action:'deny'}});
 win.webContents.on('will-navigate',(e,u)=>{if(!u.startsWith(URL_)&&!u.startsWith('file:')){e.preventDefault();shell.openExternal(u)}});
 win.once('ready-to-show',()=>win.showInactive())}
function toggleIsland(){if(!win)return;win.show();win.setIgnoreMouseEvents(false);win.focus();win.webContents.send('toggle')}
function menu(){const ds=screen.getAllDisplays();return Menu.buildFromTemplate([
 {label:'Open / close island',click:toggleIsland},
 {label:win&&win.isVisible()?'Hide ORBI':'Show ORBI',click:()=>{win.isVisible()?win.hide():win.showInactive();tray.setContextMenu(menu())}},
 {label:'Show on display',submenu:ds.map((d,i)=>({label:`Display ${i+1} (${d.bounds.width}×${d.bounds.height})`,type:'radio',checked:disp().id===d.id,click:()=>{cfg.display=d.id;save();place()}}))},
 {label:'Start with Windows',type:'checkbox',checked:app.getLoginItemSettings().openAtLogin,click:i=>app.setLoginItemSettings({openAtLogin:i.checked})},
 {type:'separator'},{label:'Quit ORBI',click:()=>app.quit()}])}
app.whenReady().then(()=>{load();create();
 tray=new Tray(nativeImage.createFromPath(path.join(__dirname,'assets','tray.png')));tray.setToolTip('ORBI — '+KEY+' to open');tray.setContextMenu(menu());tray.on('click',toggleIsland);
 globalShortcut.register(KEY,toggleIsland);
 ipcMain.on('ignore',(e,v)=>{if(win)win.setIgnoreMouseEvents(v,{forward:true})});
 ['display-metrics-changed','display-added','display-removed'].forEach(ev=>screen.on(ev,()=>{place();tray.setContextMenu(menu())}))});
app.on('second-instance',toggleIsland);
app.on('will-quit',()=>globalShortcut.unregisterAll());
app.on('window-all-closed',()=>app.quit());
