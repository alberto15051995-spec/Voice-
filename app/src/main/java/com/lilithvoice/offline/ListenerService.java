package com.lilithvoice.offline;

import android.app.*;
import android.content.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import java.util.Locale;

public class ListenerService extends Service {
  public static final String START="com.lilithvoice.offline.START", STOP="com.lilithvoice.offline.STOP";
  private static final String CHANNEL="lilith_status", ALERTS="lilith_detected";
  private static final int STATUS_ID=77;
  private volatile boolean running=false;
  private Thread worker;
  private AudioRecord recorder;
  private Model model;
  private long lastTrigger=0;

  @Override public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Estado de Lilith Voice",NotificationManager.IMPORTANCE_LOW));nm.createNotificationChannel(new NotificationChannel(ALERTS,"Frase detectada",NotificationManager.IMPORTANCE_HIGH));}
  @Override public int onStartCommand(Intent intent,int flags,int id){
    if(intent!=null && STOP.equals(intent.getAction())){halt();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
    if(running)return START_NOT_STICKY;
    running=true;
    try {startForeground(STATUS_ID,notification("Preparando modelo español…"));}
    catch(Exception ex){running=false;stopSelf();return START_NOT_STICKY;}
    worker=new Thread(this::listen,"lilith-audio");worker.start();return START_NOT_STICKY;
  }
  private Notification notification(String message){
    Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    Intent stop=new Intent(this,ListenerService.class).setAction(STOP);
    PendingIntent ps=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Lilith Voice · Escucha activa").setContentText(message).setContentIntent(pi).addAction(android.R.drawable.ic_media_pause,"Detener",ps).setOngoing(true).build();
  }
  private void status(String s){getSystemService(NotificationManager.class).notify(STATUS_ID,notification(s));}
  private void listen(){
    Recognizer rec=null;
    try {
      File folder=new File(getFilesDir(),"spanish-model");
      if(!new File(folder,"am").isDirectory()){
        status("Instalando modelo local (primera vez)…");copyAssetTree("model-es",folder);
      }
      if(!running)return;
      model=new Model(folder.getAbsolutePath());
      rec=new Recognizer(model,16000.0f);
      int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
      if(min<=0)throw new IllegalStateException("El micrófono no admite 16 kHz");
      recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192));
      if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("No se pudo preparar el micrófono");
      recorder.startRecording();status("Escuchando «Oye ChatGPT»…");
      byte[] buf=new byte[4096];
      while(running){int n=recorder.read(buf,0,buf.length);if(n>0 && rec.acceptWaveForm(buf,n))handle(rec.getResult());}
    }catch(Exception e){if(running)status("Error: "+e.getClass().getSimpleName()+": "+e.getMessage());}
    finally {running=false;try{if(recorder!=null){if(recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)recorder.stop();recorder.release();}}catch(Exception ignored){} recorder=null;try{if(rec!=null)rec.close();}catch(Exception ignored){}try{if(model!=null)model.close();}catch(Exception ignored){}model=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
  }
  private void handle(String json){
    try {String s=new JSONObject(json).optString("text","").toLowerCase(Locale.ROOT).replaceAll("[^a-záéíóúñ0-9 ]"," ").replaceAll("\s+"," ").trim();
      boolean cue=(s.contains("oye")||s.contains("oi")) && (s.contains("chat")||s.contains("chet")) && (s.contains("gpt")||s.contains("ge pe te")||s.contains("gepete")||s.contains("gp t"));
      if(cue && System.currentTimeMillis()-lastTrigger>8000){lastTrigger=System.currentTimeMillis();detected();}
    }catch(Exception ignored){}
  }
  private void detected(){
    Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);open.putExtra("detected",true);
    PendingIntent pi=PendingIntent.getActivity(this, 8,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    Notification n=new Notification.Builder(this,ALERTS).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("¡Oye ChatGPT detectado!").setContentText("Toca para abrir ChatGPT (puede requerir desbloqueo)").setAutoCancel(true).setContentIntent(pi).build();
    getSystemService(NotificationManager.class).notify(78,n);
    try{Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(vibrator!=null)vibrator.vibrate(VibrationEffect.createOneShot(130,VibrationEffect.DEFAULT_AMPLITUDE));}catch(Exception ignored){}
  }
  public static void openChatGPT(Context c){
    PackageManager pm=c.getPackageManager();Intent i=pm.getLaunchIntentForPackage("com.openai.chatgpt");
    if(i==null){android.widget.Toast.makeText(c,"Instala ChatGPT oficial primero",android.widget.Toast.LENGTH_LONG).show();return;}
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(i);}catch(Exception e){android.widget.Toast.makeText(c,"Desbloquea el móvil para abrir ChatGPT",android.widget.Toast.LENGTH_LONG).show();}
  }
  private void copyAssetTree(String assets,File dest)throws IOException{
    String[] names=getAssets().list(assets);if(names==null)throw new IOException("Falta el modelo español en los assets");
    if(names.length==0){dest.getParentFile().mkdirs();try(InputStream in=getAssets().open(assets);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return;}
    if(!dest.exists()&&!dest.mkdirs())throw new IOException("No se pudo crear "+dest);
    for(String name:names)copyAssetTree(assets+"/"+name,new File(dest,name));
  }
  private void halt(){running=false;try{if(recorder!=null)recorder.stop();}catch(Exception ignored){}}
  @Override public void onDestroy(){halt();super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
}
