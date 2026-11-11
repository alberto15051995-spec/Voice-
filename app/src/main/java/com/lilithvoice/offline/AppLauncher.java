package com.lilithvoice.offline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AppLauncher {
  private AppLauncher() {}
  public static String norm(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
      .replaceAll("\\p{M}","").replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
  public static String offer(Context context,String request) {
    String target=norm(request);
    if(target.isEmpty())return "No he entendido qué aplicación quieres abrir.";
    PackageManager pm=context.getPackageManager();
    Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> found=pm.queryIntentActivities(launcher,0);
    List<ResolveInfo> exact=new ArrayList<>(), partial=new ArrayList<>();
    for(ResolveInfo info:found){
      if(info.activityInfo==null||info.activityInfo.packageName.equals(context.getPackageName()))continue;
      String label=norm(info.loadLabel(pm).toString());
      if(label.equals(target))exact.add(info);
      else if(label.startsWith(target+" ")||target.startsWith(label+" "))partial.add(info);
    }
    List<ResolveInfo> candidates=!exact.isEmpty()?exact:partial;
    if(candidates.isEmpty())return "No encuentro la aplicación "+request+". Comprueba que esté instalada.";
    if(candidates.size()>1)return "Hay varias aplicaciones que podrían coincidir. Dime el nombre completo.";
    ResolveInfo selected=candidates.get(0);
    Intent intent=pm.getLaunchIntentForPackage(selected.activityInfo.packageName);
    if(intent==null)return "Esa aplicación no permite abrirse desde Lilith.";
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent pending=PendingIntent.getActivity(context,selected.activityInfo.packageName.hashCode(),intent,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    String label=selected.loadLabel(pm).toString();
    Notification notification=new Notification.Builder(context,"lilith_launch")
      .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Lilith · Abrir "+label)
      .setContentText("Toca para confirmar y abrir "+label)
      .setContentIntent(pending).setAutoCancel(true).build();
    context.getSystemService(NotificationManager.class).notify(150,notification);
    return "He encontrado "+label+". Toca la notificación para abrirla.";
  }
}
