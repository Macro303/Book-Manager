const headers = {
  "Accept": "application/json; charset=UTF-8",
  "Content-Type": "application/json; charset=UTF-8",
};

function addLoading(caller){
  let element = document.getElementById(caller);
  element.classList.add("is-loading");
}

function removeLoading(caller){
  let element = document.getElementById(caller);
  element.classList.remove("is-loading");
}

function getCookie(cname) {
  let name = cname + "=";
  let decodedCookie = decodeURIComponent(document.cookie);
  let ca = decodedCookie.split(';');
  for(let i = 0; i <ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}

function ready(fn) {
  if (document.readyState !== 'loading') {
    fn();
    return;
  }
  document.addEventListener('DOMContentLoaded', fn);
}

function signOut(){
  const caller = "sign-out-button"
  addLoading(caller);

  document.cookie = "token=0;path=/;max-age=0";
  window.location = "/";

  removeLoading(caller);
}
