package com.webxplorer.pro

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
 private lateinit var web: WebView
 private lateinit var address: EditText
 private lateinit var status: TextView
 private lateinit var root: LinearLayout
 private val prefs by lazy { getSharedPreferences("browser", MODE_PRIVATE) }
 private var privateMode = false
 private val adHosts = setOf("doubleclick.net","googlesyndication.com","adservice.google.com","adnxs.com","taboola.com","outbrain.com")
 private val trackerHosts = setOf("google-analytics.com","googletagmanager.com","facebook.net","hotjar.com","segment.io","mixpanel.com","clarity.ms")
 private fun enabled(k:String)=prefs.getBoolean(k,true)
 private fun save(k:String,v:Boolean)=prefs.edit().putBoolean(k,v).apply()
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private val dark get()=prefs.getBoolean("dark",true)
 private val accent get()=prefs.getInt("accent",Color.rgb(76,125,255))
 private fun bg()=if(dark) Color.rgb(12,16,29) else Color.rgb(246,248,252)
 private fun panel()=if(dark) Color.rgb(24,31,51) else Color.WHITE
 private fun fg()=if(dark) Color.WHITE else Color.rgb(22,28,42)
 private fun btn(t:String,a:()->Unit)=Button(this).apply{text=t;setTextColor(fg());setBackgroundColor(panel());setOnClickListener{a()}}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);buildUi();home()}
 private fun buildUi(){
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg())}
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(9),dp(6),dp(9),dp(6))}
  top.addView(TextView(this).apply{text="◉";textSize=25f;setTextColor(accent);setPadding(0,0,dp(7),0)})
  address=EditText(this).apply{singleLine=true;textSize=14f;setTextColor(fg());hintTextColor=Color.GRAY;hint="Search or enter address";setPadding(dp(10),0,dp(10),0);setBackgroundColor(panel());imeOptions=EditorInfo.IME_ACTION_GO}
  top.addView(address,LinearLayout.LayoutParams(0,dp(44),1f));top.addView(btn("GO"){navigate(address.text.toString())});root.addView(top)
  status=TextView(this).apply{text="  🛡 Cookies  Ads  Trackers";textSize=11f;setTextColor(accent);setPadding(dp(12),dp(4),dp(12),dp(4))};root.addView(status)
  web=WebView(this);web.settings.javaScriptEnabled=true;web.settings.domStorageEnabled=true;web.settings.loadsImagesAutomatically=true;web.settings.javaScriptCanOpenWindowsAutomatically=false;web.settings.setSupportMultipleWindows(false);web.settings.allowFileAccess=false;web.settings.allowContentAccess=false;web.settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
  CookieManager.getInstance().setAcceptCookie(enabled("cookies"))
  web.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?):Boolean{val u=r?.url?:return false;return u.scheme!="http"&&u.scheme!="https"}
   override fun shouldInterceptRequest(v:WebView?,r:WebResourceRequest?):WebResourceResponse?{val h=r?.url?.host?.lowercase(Locale.ROOT)?:return null;val block=(enabled("ads")&&adHosts.any{h==it||h.endsWith("."+it)})||(enabled("trackers")&&trackerHosts.any{h==it||h.endsWith("."+it)});return if(block)WebResourceResponse("text/plain","utf-8",null) else null}
   override fun onPageStarted(v:WebView?,u:String?,i:android.graphics.Bitmap?){address.setText(u?:"");status.text="  🛡 "+summary()+"  • Loading"}
   override fun onPageFinished(v:WebView?,u:String?){if(u!=null&&!privateMode)prefs.edit().putString("history",(u+"\n"+(prefs.getString("history","")?:"")).take(20000)).apply();status.text="  🛡 "+summary()}
  }
  web.setDownloadListener{u,_,_,_,_->try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)))}catch(_:Exception){toast("Cannot open download")}}
  root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
  val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setBackgroundColor(panel())}
  nav.addView(btn("⌂"){home()},LinearLayout.LayoutParams(0,dp(50),1f));nav.addView(btn("←"){if(web.canGoBack())web.goBack()},LinearLayout.LayoutParams(0,dp(50),1f));nav.addView(btn("＋"){web.loadUrl("about:blank");address.setText("")},LinearLayout.LayoutParams(0,dp(50),1f));nav.addView(btn("☆"){bookmark()},LinearLayout.LayoutParams(0,dp(50),1f));nav.addView(btn("☰"){menu()},LinearLayout.LayoutParams(0,dp(50),1f));root.addView(nav);setContentView(root)
  address.setOnEditorActionListener{_,id,_->if(id==EditorInfo.IME_ACTION_GO){navigate(address.text.toString());true}else false}
 }
 private fun summary()="Cookies "+(if(enabled("cookies"))"ON" else "OFF")+"  Ads "+(if(enabled("ads"))"ON" else "OFF")+"  Trackers "+(if(enabled("trackers"))"ON" else "OFF")
 private fun navigate(raw:String){var s=raw.trim();if(s.isEmpty())return;if(!s.contains("://"))s=if(s.contains(".")&&!s.contains(" "))"https://"+s else search(s);web.loadUrl(s)}
 private fun search(q:String):String{val e=prefs.getString("engine","Google");val b=when(e){"Bing"->"https://www.bing.com/search?q=";"DuckDuckGo"->"https://duckduckgo.com/?q=";"Yahoo"->"https://search.yahoo.com/search?p=";"Brave"->"https://search.brave.com/search?q=";else->"https://www.google.com/search?q="};return b+Uri.encode(q)}
 private fun home(){val back=if(dark)"#0c101d" else "#f6f8fc";val fore=if(dark)"white" else "#161c2a";web.loadDataWithBaseURL(null,"<meta name='viewport' content='width=device-width,initial-scale=1'><body style='background:"+back+";color:"+fore+";font-family:sans-serif;text-align:center;padding:32px'><h1 style='color:#4c7dff'>◉ WebXplorer Pro</h1><p>Private browsing, on your terms.</p><p>Search or enter a website above.</p><p style='margin-top:38px;color:#4c7dff'>COOKIES　 ADS　 TRACKERS</p></body>","text/html","UTF-8",null)}
 private fun bookmark(){val u=web.url?:return;val old=prefs.getString("bookmarks","")?:"";prefs.edit().putString("bookmarks",(u+"\n"+old).take(10000)).apply();toast("Bookmark saved")}
 private fun menu(){val items=arrayOf("Settings & protections","Bookmarks","History","Downloads","New incognito session","Clear browsing data","About");AlertDialog.Builder(this).setTitle("WebXplorer Pro").setItems(items){_,i->when(i){0->settings();1->showSaved("Bookmarks","bookmarks");2->showSaved("History","history");3->toast("Downloads are handled by Android");4->{privateMode=true;CookieManager.getInstance().removeAllCookies(null);web.clearHistory();home();toast("Private session started; history will not be saved")};5->clearData();6->AlertDialog.Builder(this).setMessage("WebXplorer Pro 1.0.0\nNo analytics or telemetry SDKs included.").setPositiveButton("OK",null).show()}}.show()}
 private fun settings(){val labels=arrayOf("Block cookies","Block ads","Block trackers","Dark theme");val keys=arrayOf("cookies","ads","trackers","dark");val checks=BooleanArray(4){prefs.getBoolean(keys[it],true)};AlertDialog.Builder(this).setTitle("Privacy & appearance").setMultiChoiceItems(labels,checks){_,i,on->prefs.edit().putBoolean(keys[i],on).apply();if(i==0)CookieManager.getInstance().setAcceptCookie(!on);status.text="  🛡 "+summary()}.setPositiveButton("Done"){_,_->recreate()}.setNeutralButton("Search engine"){_,_->chooseEngine()}.setNegativeButton("Clear DNS cache"){_,_->toast("Android manages DNS cache; app-level flush is unavailable")}.show()}
 private fun chooseEngine(){val e=arrayOf("Google","Bing","DuckDuckGo","Yahoo","Brave");AlertDialog.Builder(this).setTitle("Search engine").setSingleChoiceItems(e,e.indexOf(prefs.getString("engine","Google"))){d,i->prefs.edit().putString("engine",e[i]).apply();d.dismiss()}.setNegativeButton("Cancel",null).show()}
 private fun showSaved(t:String,k:String){val a=(prefs.getString(k,"")?:"").split("\n").filter{it.isNotBlank()};if(a.isEmpty()){toast("Nothing saved yet");return};AlertDialog.Builder(this).setTitle(t).setItems(a.toTypedArray()){_,i->navigate(a[i])}.setNegativeButton("Close",null).show()}
 private fun clearData(){AlertDialog.Builder(this).setTitle("Delete browsing data?").setMessage("Clears history, cookies, cache, bookmarks and saved browser preferences. Android-managed downloads are not deleted.").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->web.clearHistory();web.clearCache(true);web.clearFormData();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();prefs.edit().clear().apply();privateMode=false;toast("Browsing data cleared");recreate()}.show()}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 override fun onBackPressed(){if(::web.isInitialized&&web.canGoBack())web.goBack()else super.onBackPressed()}
 override fun onDestroy(){if(::web.isInitialized)web.destroy();super.onDestroy()}
}
