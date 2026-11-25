const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('orbiDesktop',{
 ignore:v=>ipcRenderer.send('ignore',!!v),
 onToggle:f=>ipcRenderer.on('toggle',()=>f()),
 dragStart:()=>ipcRenderer.send('dragstart'),
 dragMove:(dx,dy,iw)=>ipcRenderer.send('dragmove',dx,dy,iw),
 dragEnd:()=>ipcRenderer.send('dragend'),
 dock:s=>ipcRenderer.send('dock',s),
 undock:iw=>ipcRenderer.send('undock',iw),
 open:o=>ipcRenderer.send('open',!!o),
 version:ipcRenderer.sendSync('version'),
 mediaStatus:()=>ipcRenderer.invoke('media-status'),
 mediaCmd:c=>ipcRenderer.send('media-cmd',c),
 update:()=>ipcRenderer.invoke('update'),
 onDock:f=>ipcRenderer.on('dock',(e,s)=>f(s))});
