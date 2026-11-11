package com.lilithvoice.offline;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
  private TextView status;
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(42, 58, 42, 30);
    layout.setBackgroundColor(0xFF14121D);
    TextView title = new TextView(this);
    title.setText("LILITH  ·  v0.3"); title.setTextColor(0xFFFF6489); title.setTextSize(29);
    title.setGravity(Gravity.CENTER);layout.addView(title);
    TextView subtitle=new TextView(this);
    subtitle.setText("Tu asistente personal\nDi: «Oye, Lilith»");
    subtitle.setTextColor(Color.WHITE);subtitle.setTextSize(18);
    subtitle.setGravity(Gravity.CENTER);subtitle.setPadding(0,22,0,34);layout.addView(subtitle);
    status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(15);
    status.setText("Activa la escucha con el teléfono desbloqueado.\nEl orbe aparecerá cuando Lilith reconozca su nombre.");
    status.setPadding(0,0,0,18);layout.addView(status);
    Button overlay=new Button(this);overlay.setText("1. PERMITIR BURBUJA FLOTANTE");layout.addView(overlay);
    overlay.setOnClickListener(v->{
      if(!Settings.canDrawOverlays(this)) {
        Intent settings=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:"+getPackageName()));startActivity(settings);
      } else {status.setText("Permiso de burbuja concedido ✓");}
    });
    Button start=new Button(this);start.setText("2. ACTIVAR OYE LILITH");layout.addView(start);
    start.setOnClickListener(v->startListening());
    Button stop=new Button(this);stop.setText("DETENER ESCUCHA");layout.addView(stop);
    stop.setOnClickListener(v->{
      startService(new Intent(this,ListenerService.class).setAction(ListenerService.STOP));
      status.setText("Escucha detenida");
    });
    TextView note=new TextView(this);
    note.setText("Lilith detecta la frase con Vosk (español, local). Al despertar, muestra un orbe rojo flotante y contesta con la voz española disponible en el sistema.\n\n"+
      "Prueba también: «Qué hora es» o «Abre ChatGPT».\n\n"+
      "La escucha requiere un servicio visible y puede detenerse por las restricciones de Android/HyperOS. No es todavía un asistente del sistema ni está garantizado su funcionamiento tras reiniciar o con el teléfono bloqueado. La voz femenina depende de las voces TTS instaladas.");
    note.setTextSize(14);note.setTextColor(0xFFCCC4D1);note.setPadding(0,28,0,0);layout.addView(note);
    setContentView(layout);
    if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 81);
  }
  private void startListening() {
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},41);return;
    }
    try {
      startForegroundService(new Intent(this,ListenerService.class).setAction(ListenerService.START));
      status.setText("Preparando el modelo español. Consulta la notificación de Lilith para ver el estado.");
    } catch(Exception e) { status.setText("No se pudo iniciar: "+e.getMessage()); }
  }
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
    super.onRequestPermissionsResult(request,permissions,results);
    if(request==41 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)startListening();
  }
}
