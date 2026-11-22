const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('orbiDesktop',{ignore:v=>ipcRenderer.send('ignore',!!v),onToggle:f=>ipcRenderer.on('toggle',()=>f())});
