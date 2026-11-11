package com.lilithvoice.offline;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
  private TextView state;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(48, 78, 48, 35); layout.setBackgroundColor(Color.rgb(19, 18, 25));
    TextView title = new TextView(this); title.setText("LILITH VOICE"); title.setTextSize(30); title.setTextColor(0xffff6262); title.setGravity(Gravity.CENTER); layout.addView(title);
    TextView sub = new TextView(this); sub.setText("Detector local: Oye ChatGPT\nEspañol · Sin cuenta · Sin enviar el audio a Internet");sub.setTextColor(Color.WHITE);sub.setTextSize(16);sub.setGravity(Gravity.CENTER);sub.setPadding(0,25,0,36);layout.addView(sub);
    state = new TextView(this);state.setText("Inicia la escucha con el móvil desbloqueado. Espera a que se cargue el modelo.");state.setTextColor(Color.WHITE);state.setTextSize(15);state.setPadding(0,0,0,20);layout.addView(state);
    Button start=new Button(this);start.setText("ACTIVAR ESCUCHA");layout.addView(start);start.setOnClickListener(v-> startListening());
    Button stop=new Button(this);stop.setText("DETENER ESCUCHA");layout.addView(stop);stop.setOnClickListener(v->{startService(new Intent(this,ListenerService.class).setAction(ListenerService.STOP));state.setText("Escucha detenida");});
    Button open=new Button(this);open.setText("ABRIR CHATGPT");layout.addView(open);open.setOnClickListener(v->ListenerService.openChatGPT(this));
    TextView note=new TextView(this);note.setText("Aviso: reconoce 'oye chat gpt' y variantes. Al detectarlo vibra y muestra una notificación que abre ChatGPT. Android puede exigir desbloquear el teléfono. No inicia por sí sola el modo voz de ChatGPT.\n\nPara mejorar la estabilidad en HyperOS: permite funcionamiento en segundo plano y batería sin restricciones.");note.setTextColor(0xffcccccc);note.setTextSize(14);note.setPadding(0,32,0,0);layout.addView(note);
    setContentView(layout);
    if(getIntent().getBooleanExtra("detected",false)) { state.setText("Frase detectada. Abriendo ChatGPT…"); ListenerService.openChatGPT(this); }
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},81);
  }
  private void startListening(){
    if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},41);return;}
    try { startForegroundService(new Intent(this,ListenerService.class).setAction(ListenerService.START));state.setText("Preparando modelo... mira la notificación para comprobar el estado"); }
    catch(Exception e){state.setText("No se pudo iniciar: "+e.getMessage());}
  }
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startListening();}
}
