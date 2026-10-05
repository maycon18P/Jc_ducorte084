(function(){
  const url="https://pkbqmkszrmukucktlwje.supabase.co";
  let key=localStorage.getItem("jc_publishable_key")||"";
  if(!key){
    key=(prompt("Configuração inicial: cole a Publishable Key do Supabase. Você só precisa fazer isso uma vez.")||"").trim();
    if(key) localStorage.setItem("jc_publishable_key",key);
  }
  if(window.Android && key){ try{ window.Android.saveSupabaseKey(key); }catch(e){} }
  window.JC_CONFIG={SUPABASE_URL:url,SUPABASE_ANON_KEY:key};
})();
