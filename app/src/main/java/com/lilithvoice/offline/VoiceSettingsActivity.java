package com.lilithvoice.offline;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ScrollView;
import java.util.Locale;

public class VoiceSettingsActivity extends Activity {
  private TextToSpeech preview;
  private SeekBar pitch, speed;
  private TextView pitchText, speedText;
  private Spinner persona, voice;
  private LinearLayout root;
  private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(17);t.setPadding(0,20,0,8);root.addView(t);return t;}
  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);
    root.setPadding(36,42,36,38);root.setBackgroundColor(0xFF17131E);scroll.addView(root);setContentView(scroll);
    TextView title=label("LILITH · VOZ v0.4");title.setTextSize(25);title.setTextColor(0xFFFF6389);title.setGravity(Gravity.CENTER);
    label("Perfil de voz");voice=new Spinner(this);String[] vs={"Misteriosa (voz española disponible)","Natural (voz española disponible)"};
    voice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vs));root.addView(voice);
    voice.setSelection(VoicePrefs.voice(this).equals("natural")?1:0);
    pitchText=label("");pitch=new SeekBar(this);pitch.setMax(80);pitch.setProgress(Math.round((VoicePrefs.pitch(this)-0.6f)*100));root.addView(pitch);
    speedText=label("");speed=new SeekBar(this);speed.setMax(100);speed.setProgress(Math.round((VoicePrefs.rate(this)-0.6f)*100));root.addView(speed);
    pitch.setOnSeekBarChangeListener(watcher(()->pitchText.setText(String.format(Locale.US,"Tono: %.2fx",0.6+pitch.getProgress()/100f))));
    speed.setOnSeekBarChangeListener(watcher(()->speedText.setText(String.format(Locale.US,"Velocidad: %.2fx",0.6+speed.getProgress()/100f))));
    pitchText.setText(String.format(Locale.US,"Tono: %.2fx",0.6+pitch.getProgress()/100f));
    speedText.setText(String.format(Locale.US,"Velocidad: %.2fx",0.6+speed.getProgress()/100f));
    label("Personalidad");persona=new Spinner(this);persona.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Traviesa","Neutra"}));
    persona.setSelection(VoicePrefs.personality(this).equals("neutra")?1:0);root.addView(persona);
    Button save=new Button(this);save.setText("GUARDAR AJUSTES");root.addView(save);save.setOnClickListener(v->{save();android.widget.Toast.makeText(this,"Ajustes guardados",0).show();});
    Button test=new Button(this);test.setText("PROBAR VOZ");root.addView(test);test.setOnClickListener(v->{save();if(preview!=null){VoicePrefs.configure(this,preview);preview.speak("Aquí estoy, picha. Soy Lilith, y esta es mi nueva voz.",TextToSpeech.QUEUE_FLUSH,null,"lilith-preview");}});
    label("La voz femenina depende de las voces españolas instaladas. Estos cambios se aplican a nuevas respuestas. Para asegurar que el servicio los cargue, detén y vuelve a activar la escucha.");
    preview=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS&&preview!=null)VoicePrefs.configure(this,preview);});
  }
  private SeekBar.OnSeekBarChangeListener watcher(Runnable r){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){r.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}};}
  private void save(){VoicePrefs.save(this,0.6f+pitch.getProgress()/100f,0.6f+speed.getProgress()/100f,voice.getSelectedItemPosition()==0?"misteriosa":"natural",persona.getSelectedItemPosition()==0?"traviesa":"neutra");}
  @Override protected void onDestroy(){if(preview!=null){preview.stop();preview.shutdown();}super.onDestroy();}
}
