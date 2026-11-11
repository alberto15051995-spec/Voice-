package com.lilithvoice.offline;

import android.app.*;
import android.content.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ListenerService extends Service {
  public static final String START="com.lilithvoice.offline.START", STOP="com.lilithvoice.offline.STOP";
  private static final String CHANNEL="lilith_status";
  private static final int STATUS_ID=77;
  private volatile boolean running=false, speaking=false, awake=false;
  private volatile long awakeUntil=0;
  private Thread worker;
  private AudioRecord recorder;
  private Model model;
  private TextToSpeech tts;
  private OrbOverlay orb;
  private long lastTrigger=0;
  private final Handler mainHandler=new Handler(Looper.getMainLooper());
  private int responseIndex=0;

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager nm=getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Lilith · Escucha",NotificationManager.IMPORTANCE_LOW));
    orb=new OrbOverlay(this);
    tts=new TextToSpeech(this, status -> {
      if(status==TextToSpeech.SUCCESS && tts!=null) {
        tts.setLanguage(new Locale("es","ES"));
        // Request a likely feminine Spanish voice when present. The system ultimately decides.
        for(android.speech.tts.Voice v:tts.getVoices()==null?java.util.Collections.<android.speech.tts.Voice>emptySet():tts.getVoices()){
          String name=v.getName().toLowerCase(Locale.ROOT);
          if(v.getLocale().getLanguage().equals("es") && (name.contains("female") || name.contains("mujer") || name.contains("femen"))){tts.setVoice(v);break;}
        }
        tts.setSpeechRate(1.02f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
          @Override public void onStart(String id) {}
          @Override public void onDone(String id) {speaking=false;mainHandler.post(()->{if(orb!=null)orb.setLabel("Te escucho...");});}
          @Override public void onError(String id) {speaking=false;}
        });
      }
    });
  }
  @Override public int onStartCommand(Intent intent,int flags,int id) {
    if(intent!=null && STOP.equals(intent.getAction())) {halt();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
    if(running)return START_NOT_STICKY;
    running=true;
    try{startForeground(STATUS_ID,notification("Preparando reconocimiento español..."));}
    catch(Exception e){running=false;stopSelf();return START_NOT_STICKY;}
    worker=new Thread(this::listen,"lilith-audio");worker.start();return START_NOT_STICKY;
  }
  private Notification notification(String message) {
    Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    Intent stop=new Intent(this,ListenerService.class).setAction(STOP);
    PendingIntent ps=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    return new Notification.Builder(this,CHANNEL)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("Lilith · Escucha activa")
        .setContentText(message).setContentIntent(pi)
        .addAction(android.R.drawable.ic_media_pause,"Detener",ps).setOngoing(true).build();
  }
  private void status(String s) {getSystemService(NotificationManager.class).notify(STATUS_ID,notification(s));}
  private void listen() {
    Recognizer recognizer=null;
    try {
      File folder=new File(getFilesDir(),"spanish-model");
      if(!new File(folder,"am").isDirectory()) {
        status("Instalando modelo local (primera ejecución)...");
        copyAssetTree("model-es",folder);
      }
      if(!running)return;
      model=new Model(folder.getAbsolutePath());
      recognizer=new Recognizer(model,16000.0f);
      int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
      if(min<=0)throw new IllegalStateException("Micrófono incompatible con 16 kHz");
      recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192));
      if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("No se pudo iniciar el micrófono");
      recorder.startRecording();status("Escuchando «Oye Lilith»...");
      byte[] buffer=new byte[4096];
      while(running){
        if(awake && System.currentTimeMillis()>awakeUntil){awake=false;mainHandler.post(()->orb.hide());}
        int n=recorder.read(buffer,0,buffer.length);
        if(n<=0)continue;
        // Do not interpret Lilith's spoken replies as new user commands.
        if(speaking)continue;
        if(recognizer.acceptWaveForm(buffer,n))process(recognizer.getResult(),false);
        else process(recognizer.getPartialResult(),true);
      }
    } catch(Exception e){if(running)status("Error: "+e.getClass().getSimpleName()+": "+e.getMessage());}
    finally {
      running=false;
      try {if(recorder!=null){if(recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)recorder.stop();recorder.release();}}catch(Exception ignored){}
      recorder=null;
      try{if(recognizer!=null)recognizer.close();}catch(Exception ignored){}
      try{if(model!=null)model.close();}catch(Exception ignored){}model=null;
      mainHandler.post(()->orb.hide());stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
  }
  private String norm(String s) {
    return Normalizer.normalize(s.toLowerCase(new Locale("es","ES")),Normalizer.Form.NFD)
      .replaceAll("\\p{M}","").replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
  }
  private void process(String json,boolean partial) {
    try {
      JSONObject obj=new JSONObject(json);
      String text=norm(obj.optString(partial?"partial":"text",""));
      if(text.isEmpty())return;
      long now=System.currentTimeMillis();
      boolean trigger=text.matches(".*\\b(oye|oi)\\s+(lilith|lilit|lili)\\b.*");
      if(!awake && trigger && now-lastTrigger>3500){
        lastTrigger=now;awake=true;awakeUntil=now+14000;
        mainHandler.post(()->orb.show("Despertando..."));
        speak((responseIndex++%2==0)?"Dime, cariño. Te escucho.":"Aquí estoy, picha. ¿Qué necesitas?");
        status("Lilith despierta. Puedes hablarle.");
        return;
      }
      // Commands only from completed utterances, after the wake word.
      if(awake && !partial && now>lastTrigger+900) {
        awakeUntil=now+10000;
        if(text.contains("hora") || text.contains("que hora")) {
          String hora=new SimpleDateFormat("HH:mm",new Locale("es","ES")).format(new Date());
          mainHandler.post(()->orb.setLabel("Respondiendo..."));
          speak("Son las "+hora.replace(":"," y ")+".");
        } else if(text.contains("abre chatgpt") || text.contains("abre chat gpt")) {
          speak("Te dejo el acceso preparado.");
          // User tap is required to open apps reliably from the background on recent Android.
          sendOpenAppNotification();
        } else if(text.contains("gracias")) {speak("Para eso estoy, miarma.");}
      }
    }catch(Exception ignored){}
  }
  private void speak(String text){
    if(tts==null)return;
    speaking=true;awakeUntil=System.currentTimeMillis()+14000;
    mainHandler.post(()->orb.setLabel("Lilith hablando..."));
    int result=tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"lilith-"+System.currentTimeMillis());
    if(result==TextToSpeech.ERROR)speaking=false;
  }
  private void sendOpenAppNotification(){
    Intent open=getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
    if(open==null)return;
    PendingIntent pi=PendingIntent.getActivity(this,88,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle("Lilith · Abrir ChatGPT").setContentText("Toca para abrir ChatGPT")
      .setContentIntent(pi).setAutoCancel(true).build();
    getSystemService(NotificationManager.class).notify(78,n);
  }
  private void copyAssetTree(String asset,File dest)throws IOException{
    String[] names=getAssets().list(asset);if(names==null)throw new IOException("Falta modelo español");
    if(names.length==0){
      File parent=dest.getParentFile();if(parent!=null)parent.mkdirs();
      try(InputStream in=getAssets().open(asset);OutputStream out=new FileOutputStream(dest)){
        byte[] bytes=new byte[32768];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
      }
      return;
    }
    if(!dest.exists()&&!dest.mkdirs())throw new IOException("No se pudo crear "+dest);
    for(String name:names)copyAssetTree(asset+"/"+name,new File(dest,name));
  }
  private void halt(){running=false;awake=false;
    if(tts!=null)tts.stop();mainHandler.post(()->orb.hide());
    try{if(recorder!=null)recorder.stop();}catch(Exception ignored){}
  }
  @Override public void onDestroy(){halt();if(tts!=null){tts.shutdown();tts=null;}super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
}
