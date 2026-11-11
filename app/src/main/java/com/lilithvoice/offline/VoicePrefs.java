package com.lilithvoice.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

public final class VoicePrefs {
  private static final String FILE = "lilith_voice_prefs";
  private VoicePrefs() {}
  public static SharedPreferences prefs(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
  public static float pitch(Context c) { return prefs(c).getFloat("pitch", 1.00f); }
  public static float rate(Context c) { return prefs(c).getFloat("rate", 1.10f); }
  public static String personality(Context c) { return prefs(c).getString("personality", "traviesa"); }
  public static String voice(Context c) { return prefs(c).getString("voice", "misteriosa"); }
  public static void save(Context c, float pitch, float rate, String voice, String personality) {
    prefs(c).edit().putFloat("pitch",pitch).putFloat("rate",rate).putString("voice",voice)
        .putString("personality",personality).apply();
  }
  public static void configure(Context c, TextToSpeech tts) {
    tts.setLanguage(new Locale("es", "ES"));
    tts.setPitch(pitch(c));
    tts.setSpeechRate(rate(c));
    Set<Voice> choices=tts.getVoices();
    if(choices==null)return;
    String mode=voice(c);
    Voice selected=null;
    for(Voice v:choices) {
      if(!v.getLocale().getLanguage().equals("es"))continue;
      String n=v.getName().toLowerCase(Locale.ROOT);
      if(n.contains("female")||n.contains("femen")||n.contains("mujer")) {selected=v;break;}
      if(selected==null)selected=v;
    }
    if(selected!=null)tts.setVoice(selected);
    // "Misteriosa" es un perfil de tono y velocidad; Android no garantiza género o timbre.
  }
}
