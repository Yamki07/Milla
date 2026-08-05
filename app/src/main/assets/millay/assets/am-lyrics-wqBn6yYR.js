import{g as Ri}from"./_commonjsHelpers-Cpj98o6Y.js";function Fi(F,tt){for(var et=0;et<tt.length;et++){const V=tt[et];if(typeof V!="string"&&!Array.isArray(V)){for(const Q in V)if(Q!=="default"&&!(Q in F)){const lt=Object.getOwnPropertyDescriptor(V,Q);lt&&Object.defineProperty(F,Q,lt.get?lt:{enumerable:!0,get:()=>V[Q]})}}}return Object.freeze(Object.defineProperty(F,Symbol.toStringTag,{value:"Module"}))}var Ce={},Ie;function zi(){if(Ie)return Ce;Ie=1;function F(m,t,e,i){var s=arguments.length,n=s<3?t:i===null?i=Object.getOwnPropertyDescriptor(t,e):i,r;if(typeof Reflect=="object"&&typeof Reflect.decorate=="function")n=Reflect.decorate(m,t,e,i);else for(var a=m.length-1;a>=0;a--)(r=m[a])&&(n=(s<3?r(n):s>3?r(t,e,n):r(t,e))||n);return s>3&&n&&Object.defineProperty(t,e,n),n}typeof SuppressedError=="function"&&SuppressedError;const tt=globalThis,et=tt.ShadowRoot&&(tt.ShadyCSS===void 0||tt.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,V=Symbol(),Q=new WeakMap;let lt=class{constructor(t,e,i){if(this._$cssResult$=!0,i!==V)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o;const e=this.t;if(et&&t===void 0){const i=e!==void 0&&e.length===1;i&&(t=Q.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&Q.set(e,t))}return t}toString(){return this.cssText}};const Pe=m=>new lt(typeof m=="string"?m:m+"",void 0,V),Me=(m,...t)=>{const e=m.length===1?m[0]:t.reduce((i,s,n)=>i+(r=>{if(r._$cssResult$===!0)return r.cssText;if(typeof r=="number")return r;throw Error("Value passed to 'css' function must be a 'css' function result: "+r+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+m[n+1],m[0]);return new lt(e,m,V)},_e=(m,t)=>{if(et)m.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(const e of t){const i=document.createElement("style"),s=tt.litNonce;s!==void 0&&i.setAttribute("nonce",s),i.textContent=e.cssText,m.appendChild(i)}},Vt=et?m=>m:m=>m instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return Pe(e)})(m):m;const{is:Re,defineProperty:Fe,getOwnPropertyDescriptor:ze,getOwnPropertyNames:De,getOwnPropertySymbols:Oe,getPrototypeOf:Ue}=Object,Tt=globalThis,Yt=Tt.trustedTypes,Ne=Yt?Yt.emptyScript:"",Be=Tt.reactiveElementPolyfillSupport,mt=(m,t)=>m,Et={toAttribute(m,t){switch(t){case Boolean:m=m?Ne:null;break;case Object:case Array:m=m==null?m:JSON.stringify(m)}return m},fromAttribute(m,t){let e=m;switch(t){case Boolean:e=m!==null;break;case Number:e=m===null?null:Number(m);break;case Object:case Array:try{e=JSON.parse(m)}catch{e=null}}return e}},Pt=(m,t)=>!Re(m,t),Kt={attribute:!0,type:String,converter:Et,reflect:!1,useDefault:!1,hasChanged:Pt};Symbol.metadata??=Symbol("metadata"),Tt.litPropertyMetadata??=new WeakMap;let ct=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=Kt){if(e.state&&(e.attribute=!1),this._$Ei(),this.prototype.hasOwnProperty(t)&&((e=Object.create(e)).wrapped=!0),this.elementProperties.set(t,e),!e.noAccessor){const i=Symbol(),s=this.getPropertyDescriptor(t,i,e);s!==void 0&&Fe(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){const{get:s,set:n}=ze(this.prototype,t)??{get(){return this[e]},set(r){this[e]=r}};return{get:s,set(r){const a=s?.call(this);n?.call(this,r),this.requestUpdate(t,a,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??Kt}static _$Ei(){if(this.hasOwnProperty(mt("elementProperties")))return;const t=Ue(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(mt("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(mt("properties"))){const e=this.properties,i=[...De(e),...Oe(e)];for(const s of i)this.createProperty(s,e[s])}const t=this[Symbol.metadata];if(t!==null){const e=litPropertyMetadata.get(t);if(e!==void 0)for(const[i,s]of e)this.elementProperties.set(i,s)}this._$Eh=new Map;for(const[e,i]of this.elementProperties){const s=this._$Eu(e,i);s!==void 0&&this._$Eh.set(s,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const s of i)e.unshift(Vt(s))}else t!==void 0&&e.push(Vt(t));return e}static _$Eu(t,e){const i=e.attribute;return i===!1?void 0:typeof i=="string"?i:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(t=>this.enableUpdating=t),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(t=>t(this))}addController(t){(this._$EO??=new Set).add(t),this.renderRoot!==void 0&&this.isConnected&&t.hostConnected?.()}removeController(t){this._$EO?.delete(t)}_$E_(){const t=new Map,e=this.constructor.elementProperties;for(const i of e.keys())this.hasOwnProperty(i)&&(t.set(i,this[i]),delete this[i]);t.size>0&&(this._$Ep=t)}createRenderRoot(){const t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return _e(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(t=>t.hostConnected?.())}enableUpdating(t){}disconnectedCallback(){this._$EO?.forEach(t=>t.hostDisconnected?.())}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$ET(t,e){const i=this.constructor.elementProperties.get(t),s=this.constructor._$Eu(t,i);if(s!==void 0&&i.reflect===!0){const n=(i.converter?.toAttribute!==void 0?i.converter:Et).toAttribute(e,i.type);this._$Em=t,n==null?this.removeAttribute(s):this.setAttribute(s,n),this._$Em=null}}_$AK(t,e){const i=this.constructor,s=i._$Eh.get(t);if(s!==void 0&&this._$Em!==s){const n=i.getPropertyOptions(s),r=typeof n.converter=="function"?{fromAttribute:n.converter}:n.converter?.fromAttribute!==void 0?n.converter:Et;this._$Em=s;const a=r.fromAttribute(e,n.type);this[s]=a??this._$Ej?.get(s)??a,this._$Em=null}}requestUpdate(t,e,i,s=!1,n){if(t!==void 0){const r=this.constructor;if(s===!1&&(n=this[t]),i??=r.getPropertyOptions(t),!((i.hasChanged??Pt)(n,e)||i.useDefault&&i.reflect&&n===this._$Ej?.get(t)&&!this.hasAttribute(r._$Eu(t,i))))return;this.C(t,e,i)}this.isUpdatePending===!1&&(this._$ES=this._$EP())}C(t,e,{useDefault:i,reflect:s,wrapped:n},r){i&&!(this._$Ej??=new Map).has(t)&&(this._$Ej.set(t,r??e??this[t]),n!==!0||r!==void 0)||(this._$AL.has(t)||(this.hasUpdated||i||(e=void 0),this._$AL.set(t,e)),s===!0&&this._$Em!==t&&(this._$Eq??=new Set).add(t))}async _$EP(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}const t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(const[s,n]of this._$Ep)this[s]=n;this._$Ep=void 0}const i=this.constructor.elementProperties;if(i.size>0)for(const[s,n]of i){const{wrapped:r}=n,a=this[s];r!==!0||this._$AL.has(s)||a===void 0||this.C(s,void 0,n,a)}}let t=!1;const e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),this._$EO?.forEach(i=>i.hostUpdate?.()),this.update(e)):this._$EM()}catch(i){throw t=!1,this._$EM(),i}t&&this._$AE(e)}willUpdate(t){}_$AE(t){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EM(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Eq&&=this._$Eq.forEach(e=>this._$ET(e,this[e])),this._$EM()}updated(t){}firstUpdated(t){}};ct.elementStyles=[],ct.shadowRootOptions={mode:"open"},ct[mt("elementProperties")]=new Map,ct[mt("finalized")]=new Map,Be?.({ReactiveElement:ct}),(Tt.reactiveElementVersions??=[]).push("2.1.2");const Mt=globalThis,Xt=m=>m,$t=Mt.trustedTypes,Qt=$t?$t.createPolicy("lit-html",{createHTML:m=>m}):void 0,Zt="$lit$",Z=`lit$${Math.random().toFixed(9).slice(2)}$`,Jt="?"+Z,Ge=`<${Jt}>`,it=document,gt=()=>it.createComment(""),ft=m=>m===null||typeof m!="object"&&typeof m!="function",_t=Array.isArray,qe=m=>_t(m)||typeof m?.[Symbol.iterator]=="function",Rt=`[ 	
\f\r]`,yt=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,te=/-->/g,ee=/>/g,st=RegExp(`>|${Rt}(?:([^\\s"'>=/]+)(${Rt}*=${Rt}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),ie=/'/g,se=/"/g,ne=/^(?:script|style|textarea|title)$/i,re=m=>(t,...e)=>({_$litType$:m,strings:t,values:e}),O=re(1),ae=re(2),dt=Symbol.for("lit-noChange"),U=Symbol.for("lit-nothing"),oe=new WeakMap,nt=it.createTreeWalker(it,129);function le(m,t){if(!_t(m)||!m.hasOwnProperty("raw"))throw Error("invalid template strings array");return Qt!==void 0?Qt.createHTML(t):t}const He=(m,t)=>{const e=m.length-1,i=[];let s,n=t===2?"<svg>":t===3?"<math>":"",r=yt;for(let a=0;a<e;a++){const o=m[a];let h,c,l=-1,p=0;for(;p<o.length&&(r.lastIndex=p,c=r.exec(o),c!==null);)p=r.lastIndex,r===yt?c[1]==="!--"?r=te:c[1]!==void 0?r=ee:c[2]!==void 0?(ne.test(c[2])&&(s=RegExp("</"+c[2],"g")),r=st):c[3]!==void 0&&(r=st):r===st?c[0]===">"?(r=s??yt,l=-1):c[1]===void 0?l=-2:(l=r.lastIndex-c[2].length,h=c[1],r=c[3]===void 0?st:c[3]==='"'?se:ie):r===se||r===ie?r=st:r===te||r===ee?r=yt:(r=st,s=void 0);const y=r===st&&m[a+1].startsWith("/>")?" ":"";n+=r===yt?o+Ge:l>=0?(i.push(h),o.slice(0,l)+Zt+o.slice(l)+Z+y):o+Z+(l===-2?a:y)}return[le(m,n+(m[e]||"<?>")+(t===2?"</svg>":t===3?"</math>":"")),i]};class bt{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let n=0,r=0;const a=t.length-1,o=this.parts,[h,c]=He(t,e);if(this.el=bt.createElement(h,i),nt.currentNode=this.el.content,e===2||e===3){const l=this.el.content.firstChild;l.replaceWith(...l.childNodes)}for(;(s=nt.nextNode())!==null&&o.length<a;){if(s.nodeType===1){if(s.hasAttributes())for(const l of s.getAttributeNames())if(l.endsWith(Zt)){const p=c[r++],y=s.getAttribute(l).split(Z),u=/([.?@])?(.*)/.exec(p);o.push({type:1,index:n,name:u[2],strings:y,ctor:u[1]==="."?Ve:u[1]==="?"?Ye:u[1]==="@"?Ke:At}),s.removeAttribute(l)}else l.startsWith(Z)&&(o.push({type:6,index:n}),s.removeAttribute(l));if(ne.test(s.tagName)){const l=s.textContent.split(Z),p=l.length-1;if(p>0){s.textContent=$t?$t.emptyScript:"";for(let y=0;y<p;y++)s.append(l[y],gt()),nt.nextNode(),o.push({type:2,index:++n});s.append(l[p],gt())}}}else if(s.nodeType===8)if(s.data===Jt)o.push({type:2,index:n});else{let l=-1;for(;(l=s.data.indexOf(Z,l+1))!==-1;)o.push({type:7,index:n}),l+=Z.length-1}n++}}static createElement(t,e){const i=it.createElement("template");return i.innerHTML=t,i}}function ht(m,t,e=m,i){if(t===dt)return t;let s=i!==void 0?e._$Co?.[i]:e._$Cl;const n=ft(t)?void 0:t._$litDirective$;return s?.constructor!==n&&(s?._$AO?.(!1),n===void 0?s=void 0:(s=new n(m),s._$AT(m,e,i)),i!==void 0?(e._$Co??=[])[i]=s:e._$Cl=s),s!==void 0&&(t=ht(m,s._$AS(m,t.values),s,i)),t}class je{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){const{el:{content:e},parts:i}=this._$AD,s=(t?.creationScope??it).importNode(e,!0);nt.currentNode=s;let n=nt.nextNode(),r=0,a=0,o=i[0];for(;o!==void 0;){if(r===o.index){let h;o.type===2?h=new vt(n,n.nextSibling,this,t):o.type===1?h=new o.ctor(n,o.name,o.strings,this,t):o.type===6&&(h=new Xe(n,this,t)),this._$AV.push(h),o=i[++a]}r!==o?.index&&(n=nt.nextNode(),r++)}return nt.currentNode=it,s}p(t){let e=0;for(const i of this._$AV)i!==void 0&&(i.strings!==void 0?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class vt{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(t,e,i,s){this.type=2,this._$AH=U,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cv=s?.isConnected??!0}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return e!==void 0&&t?.nodeType===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=ht(this,t,e),ft(t)?t===U||t==null||t===""?(this._$AH!==U&&this._$AR(),this._$AH=U):t!==this._$AH&&t!==dt&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):qe(t)?this.k(t):this._(t)}O(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}_(t){this._$AH!==U&&ft(this._$AH)?this._$AA.nextSibling.data=t:this.T(it.createTextNode(t)),this._$AH=t}$(t){const{values:e,_$litType$:i}=t,s=typeof i=="number"?this._$AC(t):(i.el===void 0&&(i.el=bt.createElement(le(i.h,i.h[0]),this.options)),i);if(this._$AH?._$AD===s)this._$AH.p(e);else{const n=new je(s,this),r=n.u(this.options);n.p(e),this.T(r),this._$AH=n}}_$AC(t){let e=oe.get(t.strings);return e===void 0&&oe.set(t.strings,e=new bt(t)),e}k(t){_t(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const n of t)s===e.length?e.push(i=new vt(this.O(gt()),this.O(gt()),this,this.options)):i=e[s],i._$AI(n),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){for(this._$AP?.(!1,!0,e);t!==this._$AB;){const i=Xt(t).nextSibling;Xt(t).remove(),t=i}}setConnected(t){this._$AM===void 0&&(this._$Cv=t,this._$AP?.(t))}}class At{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,i,s,n){this.type=1,this._$AH=U,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=n,i.length>2||i[0]!==""||i[1]!==""?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=U}_$AI(t,e=this,i,s){const n=this.strings;let r=!1;if(n===void 0)t=ht(this,t,e,0),r=!ft(t)||t!==this._$AH&&t!==dt,r&&(this._$AH=t);else{const a=t;let o,h;for(t=n[0],o=0;o<n.length-1;o++)h=ht(this,a[i+o],e,o),h===dt&&(h=this._$AH[o]),r||=!ft(h)||h!==this._$AH[o],h===U?t=U:t!==U&&(t+=(h??"")+n[o+1]),this._$AH[o]=h}r&&!s&&this.j(t)}j(t){t===U?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}}class Ve extends At{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===U?void 0:t}}class Ye extends At{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==U)}}class Ke extends At{constructor(t,e,i,s,n){super(t,e,i,s,n),this.type=5}_$AI(t,e=this){if((t=ht(this,t,e,0)??U)===dt)return;const i=this._$AH,s=t===U&&i!==U||t.capture!==i.capture||t.once!==i.once||t.passive!==i.passive,n=t!==U&&(i===U||s);s&&this.element.removeEventListener(this.name,this,i),n&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){typeof this._$AH=="function"?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t)}}class Xe{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){ht(this,t)}}const Qe=Mt.litHtmlPolyfillSupport;Qe?.(bt,vt),(Mt.litHtmlVersions??=[]).push("3.3.2");const Ze=(m,t,e)=>{const i=e?.renderBefore??t;let s=i._$litPart$;if(s===void 0){const n=e?.renderBefore??null;i._$litPart$=s=new vt(t.insertBefore(gt(),n),n,void 0,e??{})}return s._$AI(m),s};const Ft=globalThis;class xt extends ct{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){const t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=Ze(e,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return dt}}xt._$litElement$=!0,xt.finalized=!0,Ft.litElementHydrateSupport?.({LitElement:xt});const Je=Ft.litElementPolyfillSupport;Je?.({LitElement:xt}),(Ft.litElementVersions??=[]).push("4.2.2");const ti={attribute:!0,type:String,converter:Et,reflect:!1,hasChanged:Pt},ei=(m=ti,t,e)=>{const{kind:i,metadata:s}=e;let n=globalThis.litPropertyMetadata.get(s);if(n===void 0&&globalThis.litPropertyMetadata.set(s,n=new Map),i==="setter"&&((m=Object.create(m)).wrapped=!0),n.set(e.name,m),i==="accessor"){const{name:r}=e;return{set(a){const o=t.get.call(this);t.set.call(this,a),this.requestUpdate(r,o,m,!0,a)},init(a){return a!==void 0&&this.C(r,void 0,m,a),a}}}if(i==="setter"){const{name:r}=e;return function(a){const o=this[r];t.call(this,a),this.requestUpdate(r,o,m,!0,a)}}throw Error("Unsupported decorator location: "+i)};function B(m){return(t,e)=>typeof e=="object"?ei(m,t,e):((i,s,n)=>{const r=s.hasOwnProperty(n);return s.constructor.createProperty(n,i),r?Object.getOwnPropertyDescriptor(s,n):void 0})(m,t,e)}function J(m){return B({...m,state:!0,attribute:!1})}const ii=(m,t,e)=>(e.configurable=!0,e.enumerable=!0,Reflect.decorate&&typeof t!="object"&&Object.defineProperty(m,t,e),e);function si(m,t){return(e,i,s)=>{const n=r=>r.renderRoot?.querySelector(m)??null;return ii(e,i,{get(){return n(this)}})}}const Y={GOOGLE:{MAX_RETRIES:3,RETRY_DELAY_MS:1e3,FETCH_TIMEOUT_MS:6e3}};class K{static delay(t){return new Promise(e=>{setTimeout(e,t)})}static fetchWithTimeout(t,e=Y.GOOGLE.FETCH_TIMEOUT_MS){const i=new AbortController,s=setTimeout(()=>i.abort(),e);return fetch(t,{signal:i.signal}).finally(()=>clearTimeout(s))}static isPurelyLatinScript(t){return/^[\u0000-\u007F\u0080-\u00FF\u0100-\u017F\u0180-\u024F]*$/.test(t)}static async translate(t,e){if(!t||Array.isArray(t)&&t.length===0)return Array.isArray(t)?[]:"";const i=Array.isArray(t),s=i?t:[t],n=[],r=[];if(s.forEach((u,f)=>{u&&u.trim()&&(n.push(f),r.push(u))}),r.length===0)return i?s:s[0];const a=1500,o=new Array(r.length).fill("");let h=[],c=[],l=0;const p=async(u,f)=>{if(u.length===0)return;const C=u.join(`
`);let v=0,S=!1;for(;v<Y.GOOGLE.MAX_RETRIES&&!S;)try{const E=`https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${e}&dt=t&q=${encodeURIComponent(C)}`,g=await K.fetchWithTimeout(E);if(!g.ok)throw new Error(`Status ${g.status}`);const T=((await g.json())?.[0]?.map(A=>A?.[0]).join("")||"").split(`
`);f.forEach((A,P)=>{P<T.length?o[A]=T[P]:o[A]=u[P]}),S=!0}catch{v+=1,v<Y.GOOGLE.MAX_RETRIES?await K.delay(Y.GOOGLE.RETRY_DELAY_MS*2**(v-1)):f.forEach((g,L)=>{o[g]=u[L]})}};for(let u=0;u<r.length;u+=1){const f=r[u];l+f.length>a&&(await p(h,c),h=[],c=[],l=0),h.push(f),c.push(u),l+=f.length}h.length>0&&await p(h,c);const y=[...s];return n.forEach((u,f)=>{y[u]=o[f]}),i?y:y[0]}static async romanize(t){const e=Array.isArray(t)?t:t.data||t.content||[];return!e||e.length===0?Array.isArray(t)?t:[]:e.some(s=>s.isWordSynced!==!1&&Array.isArray(s.text)&&s.text.length>1)?this.romanizeWordSynced(e):this.romanizeLineSynced(e)}static async romanizeWordSynced(t){return Promise.all(t.map(async e=>{if(!e.text||!Array.isArray(e.text)||e.text.length===0||e.romanizedText)return e;const i=e.text.map(r=>r.text).join(""),[s]=await this.romanizeTexts([i]),n=e.text.map(r=>({...r,romanizedText:r.romanizedText}));return{...e,text:n,romanizedText:s||""}}))}static async romanizeLineSynced(t){const e=t.map(s=>s.romanizedText?"":Array.isArray(s.text)&&s.text.length>0?s.text.map(n=>n.text).join(""):""),i=await this.romanizeTexts(e);return t.map((s,n)=>({...s,romanizedText:i[n]||""}))}static async romanizeTexts(t){const e=t.join(" ");if(K.isPurelyLatinScript(e))return t;const i=[];for(const s of t)if(!s||K.isPurelyLatinScript(s))i.push(s);else{let n=0,r=!1,a=null;for(;n<Y.GOOGLE.MAX_RETRIES&&!r;)try{const o=`https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=rm&q=${encodeURIComponent(s)}`,l=(await(await K.fetchWithTimeout(o)).json())?.[0]?.[0]?.[3]||s;i.push(l),r=!0}catch(o){a=o,console.warn(`GoogleService: Error romanizing text "${s}" (attempt ${n+1}/${Y.GOOGLE.MAX_RETRIES}):`,o),n+=1,n<Y.GOOGLE.MAX_RETRIES&&await K.delay(Y.GOOGLE.RETRY_DELAY_MS*2**(n-1))}r||(console.error(`GoogleService: Failed to romanize text "${s}" after ${Y.GOOGLE.MAX_RETRIES} attempts. Last error:`,a),i.push(s))}return i}}const ce="1.5.6",wt=7e3,ni=8e3,ri=500,zt=350,Dt=4e3,ut=Dt*2,pt=600,ai=.85,oi=180,li=80,ci=240,de=.75,di=.45,hi=.35,ui=760,pi=1320,he=100;function X(m,t={},e=ni){const i=new AbortController,s=setTimeout(()=>i.abort(),e);return fetch(m,{...t,signal:i.signal}).finally(()=>clearTimeout(s))}const ue=["https://lyricsplus.binimum.org","https://lyricsplus-seven.vercel.app","https://lyricsplus.prjktla.workers.dev","https://lyrics-plus-backend.vercel.app"],pe="apple,lyricsplus,musixmatch,spotify,qq,deezer,musixmatch-word",mi="https://fetch-genius.samidy.workers.dev/";class d extends xt{constructor(){super(...arguments),this.downloadFormat="auto",this.highlightColor="#ffffff",this.autoScroll=!0,this.interpolate=!0,this.showRomanization=!1,this.showTranslation=!1,this._currentTime=0,this.isLoading=!1,this.activeLineIndices=[],this.activeMainWordIndices=new Map,this.activeBackgroundWordIndices=new Map,this.mainWordProgress=new Map,this.backgroundWordProgress=new Map,this.lyricsSource=null,this.availableSources=[],this.currentSourceIndex=0,this.isFetchingAlternatives=!1,this.hasFetchedAllProviders=!1,this.mainWordAnimations=new Map,this.backgroundWordAnimations=new Map,this.lastInstrumentalIndex=null,this.isUserScrolling=!1,this.isProgrammaticScroll=!1,this.isClickSeeking=!1,this.cachedLyricsLines=[],this.cachedLineArray=[],this.lineElementCache=new Map,this.gapElementCache=new Map,this.cachedAllGaps=[],this.cachedIsUnsynced=!1,this.cachedLineData=null,this.activeLineIds=new Set,this.currentPrimaryActiveLine=null,this.lastPrimaryActiveLine=null,this.backgroundExpandedLine=null,this.scrollAnimationState=null,this.currentScrollOffset=0,this.animatingLines=[],this.lastActiveIndex=0,this.visibleLineIds=new Set,this.cachedScrollPaddingTop=null,this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this._boundHandleUserScroll=this.handleUserScroll.bind(this),this._boundAnimateProgress=this.animateProgress.bind(this)}async toggleRomanization(){this.showRomanization=!this.showRomanization,await this.applyRomanization()}async applyRomanization(){if(this.showRomanization&&this.lyrics&&this.lyrics.some(e=>!e.romanizedText&&(!e.text||!e.text.some(i=>i.romanizedText)))){this.isLoading=!0;try{const e=await K.romanize(this.lyrics);this.lyrics=e}catch(e){console.error("Romanization failed",e)}finally{this.isLoading=!1}}}async toggleTranslation(){this.showTranslation=!this.showTranslation,await this.applyTranslation()}async applyTranslation(){if(this.showTranslation&&this.lyrics&&this.lyrics.some(e=>!e.translation)){this.isLoading=!0;try{const e=this.lyrics.map(r=>r.translation?"":r.text.map(a=>a.text).join(""));if(e.every(r=>!r)){this.isLoading=!1;return}const i=await K.translate(e,"en"),s=Array.isArray(i)?i:[i],n=this.lyrics.map((r,a)=>r.translation?r:{...r,translation:s[a]||void 0});this.lyrics=n}catch(e){console.error("Translation failed",e)}finally{this.isLoading=!1}}}set currentTime(t){const e=this._currentTime;t<e&&e-t>1e3&&this.lyrics&&(this.activeLineIndices=[],this.activeMainWordIndices.clear(),this.activeBackgroundWordIndices.clear(),this.mainWordProgress.clear(),this.backgroundWordProgress.clear(),this.mainWordAnimations.clear(),this.backgroundWordAnimations.clear(),this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this.clearBackgroundExpandedLine(),this.lyricsContainer&&(this.lyricsContainer.querySelectorAll(".lyrics-line.active, .lyrics-line.pre-active, .lyrics-line.bg-expanded, .lyrics-line.scroll-exiting").forEach(n=>{n.classList.remove("active","pre-active","bg-expanded","scroll-exiting"),d.resetSyllables(n)}),this.lyricsContainer.querySelectorAll(".lyrics-gap.active, .lyrics-gap.gap-exiting").forEach(n=>n.classList.remove("active","gap-exiting")),this.gapElementCache.clear())),this._currentTime=t,e!==t&&this.lyrics&&this._onTimeChanged(e,t)}get currentTime(){return this._currentTime}_updateFooter(){const t=this.shadowRoot?.querySelector(".lyrics-footer");if(!t)return;const e=t.querySelector(".source-switch-btn"),i=t.querySelector(".source-switch-svg"),s=t.querySelector(".source-switch-label");e&&(e.disabled=this.isFetchingAlternatives),i&&i.classList.toggle("is-loading",this.isFetchingAlternatives),s&&(s.textContent=this.isFetchingAlternatives?"Switching...":"Switch")}connectedCallback(){super.connectedCallback(),this.fetchLyrics()}disconnectedCallback(){super.disconnectedCallback(),this.animationFrameId&&(cancelAnimationFrame(this.animationFrameId),this.animationFrameId=void 0),this.userScrollTimeoutId&&(clearTimeout(this.userScrollTimeoutId),this.userScrollTimeoutId=void 0),this.clickSeekTimeout&&(clearTimeout(this.clickSeekTimeout),this.clickSeekTimeout=void 0),this.scrollAnimationTimeout&&(clearTimeout(this.scrollAnimationTimeout),this.scrollAnimationTimeout=void 0),this.scrollUnlockTimeout&&(clearTimeout(this.scrollUnlockTimeout),this.scrollUnlockTimeout=void 0),this.fetchAbortController?.abort(),this.fetchAbortController=void 0,this.lyricsContainer&&(this.lyricsContainer.removeEventListener("wheel",this._boundHandleUserScroll),this.lyricsContainer.removeEventListener("touchmove",this._boundHandleUserScroll)),this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this.visibilityObserver?.disconnect(),this.visibilityObserver=void 0}async fetchLyrics(){this.fetchAbortController?.abort();const t=new AbortController;this.fetchAbortController=t,this.isLoading=!0,this.lyrics=void 0,this.lyricsSource=null,this.availableSources=[],this.currentSourceIndex=0,this.isFetchingAlternatives=!1,this.hasFetchedAllProviders=!1,this._updateFooter();try{if(this.ttml){const r=d.parseTTML(this.ttml);if(r&&r.lines.length>0){this.lyrics=r.lines,this.lyricsSource="Local",r.songwriters&&(this.songwriters=r.songwriters),this.availableSources=[{lines:this.lyrics,source:"Local",songwriters:this.songwriters}],this.currentSourceIndex=0,this.hasFetchedAllProviders=!0,this._updateFooter(),await this.onLyricsLoaded();return}}const e=await this.resolveSongMetadata();if(t.signal.aborted)return;const i=!!this.musicId&&!this.songTitle&&!this.songArtist&&!this.query&&!this.isrc,s=[];if(e?.metadata&&!i){const r=e.metadata.title?.trim()||"",a=e.metadata.artist?.trim()||"",o=await d.fetchLyricsFromBiniLyrics(r,a,e.catalogIsrc,e.metadata);o&&o.lines.length>0&&s.push(o);const h=c=>c.some(l=>l.lines.some(p=>p.isWordSynced||p.text&&p.text.length>1));if(s.length===0||!h(s)){const c=await d.fetchLyricsFromUnison(e.metadata);c&&c.lines.length>0&&s.push(c)}if(s.length===0||!h(s)){const c=await d.fetchLyricsFromYouLyPlus(r,a,e.catalogIsrc,e.metadata,!0);c&&c.length>0&&s.push(...c)}}const n=r=>r.some(a=>a.lines.some(o=>o.timestamp>0||o.endtime>0));if((s.length===0||!n(s))&&e?.metadata){const r=await d.fetchLyricsFromLrclib(e.metadata);r&&r.lines.length>0&&s.push({lines:r.lines,source:"LRCLIB"})}if(s.length===0&&e?.metadata){const r=await d.fetchLyricsFromGenius(e.metadata);r&&r.lines.length>0&&s.push({lines:r.lines,source:"Genius"})}if(this.hasFetchedAllProviders=s.length===0||s.some(r=>r.source==="LRCLIB"||r.source==="Genius"),this._updateFooter(),s.length>0){this.availableSources=d.mergeAndSortSources(s),this.currentSourceIndex=0;const r=this.availableSources[0];this.lyrics=r.lines,this.lyricsSource=r.source,r.songwriters&&(this.songwriters=r.songwriters),await this.onLyricsLoaded();return}this.lyrics=void 0,this.lyricsSource=null}finally{t.signal.aborted||(this.isLoading=!1)}}async onLyricsLoaded(){this.activeLineIndices=[],this.activeMainWordIndices.clear(),this.activeBackgroundWordIndices.clear(),this.mainWordProgress.clear(),this.backgroundWordProgress.clear(),this.mainWordAnimations.clear(),this.backgroundWordAnimations.clear(),this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this.clearBackgroundExpandedLine(),this.lyricsContainer&&(this.isProgrammaticScroll=!0,this.lyricsContainer.scrollTop=0,window.setTimeout(()=>{this.isProgrammaticScroll=!1},100)),await this.autoProcessLyrics()}async autoProcessLyrics(){this.showRomanization&&await this.applyRomanization(),this.showTranslation&&await this.applyTranslation()}static getRankForCollected(t,e){const i=t.toLowerCase(),s=e.some(a=>a.text&&Array.isArray(a.text)&&a.text.length>1),n=e.length>0&&e.every(a=>a.timestamp===0&&a.endtime===0),r=i.includes("qq")||i.includes("lyricsplus");return i.includes("apple")&&s?1:i.includes("bini")&&s?2:i.includes("unison")&&s?3:r&&s?4:i.includes("musixmatch")&&s?5:i.includes("lrclib")&&s?6:s?7:i.includes("apple")&&!s&&!n?8:i.includes("bini")&&!s&&!n?9:i.includes("unison")&&!s&&!n?10:r&&!s&&!n?11:i.includes("musixmatch")&&!s&&!n?12:i.includes("lrclib")&&!s&&!n?13:!s&&!n?14:i.includes("apple")&&n?15:i.includes("bini")&&n?16:i.includes("unison")&&n?17:r&&n?18:i.includes("musixmatch")&&n?19:i.includes("lrclib")&&n?20:i.includes("genius")?21:30}static getDisplaySourceLabel(t){return t.toLowerCase().includes("lyricsplus")?"QQ":t}static getSourceKey(t){const e=(t||"").trim().toLowerCase();return e?e.includes("lyricsplus")||e==="qq"?"qq":e.replace(/\s+/g," "):""}static mergeAndSortSources(t){const e=new Map;for(const i of t){const s=d.getDisplaySourceLabel(i.source);e.has(s)||e.set(s,{...i,source:s})}return Array.from(e.values()).sort((i,s)=>d.getRankForCollected(i.source,i.lines)-d.getRankForCollected(s.source,s.lines))}findCurrentSourceIndex(t=this.availableSources,e=this.lyricsSource,i=this.lyrics){const s=t.findIndex(r=>r.lines===i);if(s!==-1)return s;const n=d.getSourceKey(e);return n?t.findIndex(r=>d.getSourceKey(r.source)===n):-1}static getNextSourceIndex(t,e,i,s){if(t.length<=1)return-1;if(e!==-1)return(e+1)%t.length;const n=d.getSourceKey(i),r=t.findIndex(a=>a.lines!==s&&d.getSourceKey(a.source)!==n);return r===-1?0:r}async applySourceAtIndex(t){const e=this.availableSources[t];e&&(this.currentSourceIndex=t,this.lyrics=e.lines,this.lyricsSource=e.source,e.songwriters&&(this.songwriters=e.songwriters),await this.onLyricsLoaded())}async switchSource(){if(this.isFetchingAlternatives)return;const t=this.lyricsSource,e=this.lyrics;if(!this.hasFetchedAllProviders){this.isFetchingAlternatives=!0,this._updateFooter();try{const i=await this.resolveSongMetadata();if(i?.metadata){const s=[];if(!this.availableSources.some(n=>n.source.toLowerCase().includes("unison"))){const n=await d.fetchLyricsFromUnison(i.metadata);n&&n.lines.length>0&&s.push(n)}if(!this.availableSources.some(n=>n.source.toLowerCase().includes("apple")||n.source.toLowerCase().includes("qq"))){const n=i.metadata.title?.trim()||"",r=i.metadata.artist?.trim()||"",a=await d.fetchLyricsFromYouLyPlus(n,r,i.catalogIsrc,i.metadata,!0);a&&a.length>0&&s.push(...a)}if(!this.availableSources.some(n=>n.source.toLowerCase().includes("lrclib"))){const n=await d.fetchLyricsFromLrclib(i.metadata);n&&n.lines.length>0&&s.push({lines:n.lines,source:"LRCLIB"})}if(!this.availableSources.some(n=>n.source.toLowerCase().includes("genius"))){const n=await d.fetchLyricsFromGenius(i.metadata);n&&n.lines.length>0&&s.push({lines:n.lines,source:"Genius"})}s.length>0&&(this.availableSources=d.mergeAndSortSources([...this.availableSources,...s]),this.currentSourceIndex=this.findCurrentSourceIndex(this.availableSources,t,e))}}finally{this.hasFetchedAllProviders=!0,this.isFetchingAlternatives=!1,this._updateFooter()}}if(this.availableSources.length>1){const i=this.findCurrentSourceIndex(this.availableSources,t,e),s=d.getNextSourceIndex(this.availableSources,i,t,e);s!==-1&&await this.applySourceAtIndex(s)}}async resolveSongMetadata(){const t={title:this.songTitle?.trim()??"",artist:this.songArtist?.trim()??"",album:this.songAlbum?.trim()||void 0,songwriters:this.songwriters?.trim()||void 0,durationMs:void 0};typeof this.songDurationMs=="number"&&this.songDurationMs>0?t.durationMs=this.songDurationMs:typeof this.duration=="number"&&this.duration>0&&(t.durationMs=this.duration);const e=null;let i=this.musicId,s=this.isrc;if(this.query&&(!t.title||!t.artist||!t.album)){const l=d.parseQueryMetadata(this.query);l&&(!t.title&&l.title&&(t.title=l.title),!t.artist&&l.artist&&(t.artist=l.artist),!t.album&&l.album&&(t.album=l.album))}let n=null;this.query&&(!t.title||!t.artist)&&(n=await d.searchLyricsPlusCatalog(this.query),n&&(!t.title&&n.title&&(t.title=n.title),!t.artist&&n.artist&&(t.artist=n.artist),!t.album&&n.album&&(t.album=n.album),!t.songwriters&&n.songwriters&&(t.songwriters=n.songwriters),t.durationMs==null&&typeof n.durationMs=="number"&&n.durationMs>0&&(t.durationMs=n.durationMs),!i&&n.id?.appleMusic&&(i=n.id.appleMusic),!s&&n.isrc&&(s=n.isrc)));const r=t.title?.trim()??"",a=t.artist?.trim()??"",o=t.album?.trim(),h=typeof t.durationMs=="number"&&Number.isFinite(t.durationMs)&&t.durationMs>0?Math.round(t.durationMs):void 0;return{metadata:r&&a?{title:r,artist:a,album:o||void 0,durationMs:h}:void 0,appleId:i,appleSong:e,catalogIsrc:s}}static parseQueryMetadata(t){const e=t?.trim();if(!e)return null;const i={},s=e.split(/\s[-–—]\s/);if(s.length>=2){const[r,...a]=s,o=a.join(" - "),h=r.trim(),c=o.trim();if(h&&c)return i.title=h,i.artist=c,i}const n=e.split(/\s+[bB]y\s+/);if(n.length===2){const[r,a]=n.map(o=>o.trim());if(r&&a)return i.title=r,i.artist=a,i}return null}static async searchLyricsPlusCatalog(t){const e=t?.trim();if(!e)return null;for(const i of ue){const n=`${i.endsWith("/")?i.slice(0,-1):i}/v1/songlist/search?q=${encodeURIComponent(e)}`;try{const r=await X(n);if(r.ok){const a=await r.json();let o=[];const h=a;if(Array.isArray(h?.results)?o=h.results:Array.isArray(a)&&(o=a),o.length>0)return o.find(l=>l?.id&&l.id.appleMusic)??o[0]}}catch{}}return null}static async fetchLyricsFromBiniLyrics(t,e,i,s={}){if((!t||!e)&&!i)return null;try{let n=null;if(i)try{const r=`https://lyrics-api.binimum.org/?isrc=${encodeURIComponent(i)}`,a=await X(r);if(a.ok){const o=await a.json();o.results&&o.results.length>0&&(n=o)}}catch{}if(!n&&t&&e){const r=new URLSearchParams({track:t,artist:e});s.album&&r.append("album",s.album),s.durationMs&&s.durationMs>0&&r.append("duration",Math.round(s.durationMs/1e3).toString());const a=`https://lyrics-api.binimum.org/?${r.toString()}`,o=await X(a);o.ok&&(n=await o.json())}if(n&&n.results&&n.results.length>0){const r=n.results[0];if(r.lyricsUrl){const a=await X(r.lyricsUrl);if(a.ok){const o=await a.text(),h=d.parseTTML(o);if(h&&h.lines.length>0)return{lines:h.lines,source:"BiniLyrics",songwriters:h.songwriters}}}}}catch(n){console.error("Cache API failed",n)}return null}static async fetchLyricsFromYouLyPlus(t,e,i,s={},n=!1){if((!t||!e)&&!i)return[];const r=new URLSearchParams;t&&r.append("title",t),e&&r.append("artist",e),i&&r.append("isrc",i),s.album&&r.append("album",s.album),s.durationMs&&s.durationMs>0&&r.append("duration",Math.round(s.durationMs/1e3).toString()),pe.includes("apple")||r.append("source",pe);const a=(l,p)=>{const y=l.toLowerCase(),u=p.some(v=>v.text&&Array.isArray(v.text)&&v.text.length>1),f=p.length>0&&p.every(v=>v.timestamp===0&&v.endtime===0),C=y.includes("qq")||y.includes("lyricsplus");return y.includes("apple")&&u?1:y.includes("bini")&&u?2:y.includes("unison")&&u?3:C&&u?4:y.includes("musixmatch")&&u?5:u?6:y.includes("apple")&&!u&&!f?7:y.includes("bini")&&!u&&!f?8:y.includes("unison")&&!u&&!f?9:C&&!u&&!f?10:y.includes("musixmatch")&&!u&&!f?11:!u&&!f?12:y.includes("apple")&&f?13:y.includes("bini")&&f?14:y.includes("unison")&&f?15:C&&f?16:y.includes("musixmatch")&&f?17:30},o=[];if(!n){const l=await d.fetchLyricsFromBiniLyrics(t,e,i,s);if(l)return o.push(l),o}const h=[...ue].sort(()=>Math.random()-.5).slice(0,3);for(const l of h){const y=`${l.endsWith("/")?l.slice(0,-1):l}/v2/lyrics/get?${r.toString()}`;let u=null;try{const f=await X(y);f.ok&&(u=await f.json())}catch{u=null}if(u){const f=d.convertKPoeLyrics(u);if(f&&f.length>0){const C=u?.metadata?.source||u?.metadata?.provider||"LyricsPlus (KPoe)",v=a(C,f),S={lines:f,source:C};if(o.push(S),v===1)break}}}if(!o.some(l=>a(l.source,l.lines)<=2))try{const p=`https://lyricsplus.binimum.org/v2/lyrics/get?${new URLSearchParams(r).toString()}`,y=await X(p);if(y.ok){const u=await y.json();if(u){const f=d.convertKPoeLyrics(u),C=u?.metadata?.source||u?.metadata?.provider||"LyricsPlus (KPoe)",v=f?.some(S=>S.text&&Array.isArray(S.text)&&S.text.length>1);f&&f.length>0&&v&&o.push({lines:f,source:C})}}}catch{}return o}static parseLrcSubtitles(t){if(!t||typeof t!="string")return[];const e=[],i=t.split(`
`),s=[];for(const n of i){const r=n.match(/^\[(\d{1,3}):(\d{2})\.(\d{2,3})\]\s?(.*)$/);if(!r)continue;const a=parseInt(r[1],10),o=parseInt(r[2],10);let h=parseInt(r[3],10);r[3].length===3&&(h=Math.round(h/10));const c=(a*60+o)*1e3+h*10,l=r[4]||"";s.push({timestamp:c,text:l})}for(let n=0;n<s.length;n+=1){const{timestamp:r,text:a}=s[n],o=n+1<s.length?s[n+1].timestamp:r+5e3;if(!a.trim())continue;const h={text:a,part:!1,timestamp:r,endtime:o,lineSynced:!0};e.push({text:[h],background:!1,backgroundText:[],oppositeTurn:!1,timestamp:r,endtime:o,isWordSynced:!1})}return e}static async fetchLyricsFromLrclib(t){const e=t.title?.trim(),i=t.artist?.trim();if(!e||!i)return null;try{const s=`${i} ${e}`,n=new URLSearchParams({q:s}),r=await X(`https://lrclib.net/api/search?${n.toString()}`,{headers:{"User-Agent":`apple-music-web-components/${ce}`}});if(!r.ok)return null;const a=await r.json();if(!Array.isArray(a)||a.length===0)return null;const h=a.find(c=>c.syncedLyrics&&typeof c.syncedLyrics=="string")||a[0];if(h.syncedLyrics){const c=d.parseLrcSubtitles(h.syncedLyrics);if(c.length>0)return{lines:c,source:"LRCLIB"}}if(h.plainLyrics&&typeof h.plainLyrics=="string"){const c=h.plainLyrics.split(`
`).filter(l=>l.trim());if(c.length>0)return{lines:c.map(p=>({text:[{text:p,part:!1,timestamp:0,endtime:0}],background:!1,backgroundText:[],oppositeTurn:!1,timestamp:0,endtime:0,isWordSynced:!1})),source:"LRCLIB (unsynced)"}}}catch{}return null}static async fetchLyricsFromGenius(t){const e=t.title?.trim(),i=t.artist?.trim();if(!e||!i)return null;try{const s=new URLSearchParams({title:e,artist:i}),n=await X(`${mi}?${s.toString()}`);if(!n.ok)return null;const r=await n.json();if(r.lyrics){const a=r.lyrics.split(`
`).map(o=>o.trim()).filter(o=>o&&!o.startsWith("["));if(a.length>0)return{lines:a.map(h=>({text:[{text:h,part:!1,timestamp:0,endtime:0}],background:!1,backgroundText:[],oppositeTurn:!1,timestamp:0,endtime:0,isWordSynced:!1})),source:"Genius"}}}catch{}return null}static async fetchLyricsFromUnison(t){const e=t.title?.trim(),i=t.artist?.trim();if(!e||!i)return null;const s=new URLSearchParams;s.append("song",e),s.append("artist",i),t.album&&s.append("album",t.album),t.durationMs&&t.durationMs>0&&s.append("duration",Math.round(t.durationMs/1e3).toString());try{const n=await X(`https://unison.boidu.dev/lyrics?${s.toString()}`);if(!n.ok)return null;const r=await n.json();if(!r.success||!r.data?.lyrics)return null;const a=r.data,o=a.format||"lrc",h=a.syncType||"linesync",c=a.lyrics;if(o==="ttml"){const l=d.parseTTML(c);if(l&&l.lines.length>0)return{lines:l.lines,source:"Unison",songwriters:l.songwriters}}if(o==="lrc")if(h==="plain"){const l=c.split(`
`).map(p=>p.trim()).filter(p=>p);if(l.length>0)return{lines:l.map(y=>({text:[{text:y,part:!1,timestamp:0,endtime:0}],background:!1,backgroundText:[],oppositeTurn:!1,timestamp:0,endtime:0,isWordSynced:!1})),source:"Unison (unsynced)"}}else{const l=d.parseLrcSubtitles(c);if(l.length>0)return{lines:l,source:"Unison"}}}catch{}return null}static calculateLineAlignments(t,e){const i=new Array(t.length).fill(void 0);let s=!0,n=null,r=0,a=0;if(t.forEach((o,h)=>{let c;if(o){let l=e[o];l||(o==="v1000"?l="group":o==="v2000"?l="other":l="person"),l==="group"?c="start":(n===null?l==="other"?s=!1:s=!0:o!==n&&(s=!s),c=s?"start":"end",n=o)}c&&(a+=1,c==="end"&&(r+=1)),i[h]=c}),a>0&&Math.round(r/a*100)>=85){const o=h=>h==="start"?"end":h==="end"?"start":h;for(let h=0;h<i.length;h+=1)i[h]=o(i[h])}return i}static parseTTML(t){try{const i=new DOMParser().parseFromString(t,"text/xml"),s={},n={},r={},a=i.getElementsByTagName("ttm:agent");for(let v=0;v<a.length;v+=1){const S=a[v],E=S.getAttribute("xml:id"),g=S.getAttribute("type");E&&g&&(r[E]=g)}let o;const h=i.getElementsByTagName("songwriter");if(h.length>0){const v=[];for(let S=0;S<h.length;S+=1)h[S].textContent&&v.push(h[S].textContent);v.length>0&&(o=v.join(", "))}const c=i.getElementsByTagName("translation");for(let v=0;v<c.length;v+=1){const S=c[v].getElementsByTagName("text");for(let E=0;E<S.length;E+=1){const g=S[E],L=g.getAttribute("for");L&&g.textContent&&(s[L]=g.textContent)}}const l=v=>{if(!v)return 0;const S=v.split(":");let E=0;return S.length===2?E=parseInt(S[0],10)*60+parseFloat(S[1]):S.length===3?E=parseInt(S[0],10)*3600+parseInt(S[1],10)*60+parseFloat(S[2]):E=parseFloat(S[0]),Math.round(E*1e3)},p=i.getElementsByTagName("transliteration");for(let v=0;v<p.length;v+=1){const S=p[v].getElementsByTagName("text");for(let E=0;E<S.length;E+=1){const g=S[E],L=g.getAttribute("for");if(!L)continue;const R=Array.from(g.getElementsByTagName("span")).filter(T=>T.getAttribute("begin"));if(R.length>0){const T=[];let A="";for(let P=0;P<R.length;P+=1){const b=R[P],x=b.getAttribute("begin"),I=b.getAttribute("end");let w=b.textContent||"";const k=b.nextSibling;k&&k.nodeType===3&&/^\s/.test(k.textContent||"")&&!w.endsWith(" ")&&(w+=" "),w.trim()!==""&&(T.push({time:l(x),duration:l(I)-l(x),text:w}),A+=w)}n[L]={text:A.trim(),syllabus:T}}else g.textContent&&(n[L]={text:g.textContent.trim().replace(/\s+/g," ")})}}const y=[],u=i.getElementsByTagName("p"),f=[];for(let v=0;v<u.length;v+=1)f.push(u[v].getAttribute("ttm:agent")||void 0);const C=d.calculateLineAlignments(f,r);for(let v=0;v<u.length;v+=1){const S=u[v],E=S.getAttribute("itunes:key"),g=l(S.getAttribute("begin")),L=l(S.getAttribute("end"));let R;S.parentNode&&S.parentNode.tagName==="div"&&(R=S.parentNode.getAttribute("itunes:songPart")||void 0);const T=[],A=[],P=S.getElementsByTagName("span");if(P.length>0)for(let I=0;I<P.length;I+=1){const w=P[I];if(w.getAttribute("ttm:role")==="x-bg"){const $=w.getElementsByTagName("span");for(let W=0;W<$.length;W+=1){const z=$[W];let M=z.textContent||"";const D=z.nextSibling;D&&D.nodeType===3&&/^\s/.test(D.textContent||"")&&!M.endsWith(" ")&&(M+=" "),A.push({text:M,timestamp:l(z.getAttribute("begin")),endtime:l(z.getAttribute("end")),part:!/\s$/.test(M)})}continue}if(w.parentNode&&w.parentNode.getAttribute?.("ttm:role")==="x-bg")continue;let k=w.textContent||"";const _=w.nextSibling;_&&_.nodeType===3&&/^\s/.test(_.textContent||"")&&!k.endsWith(" ")&&(k+=" "),T.push({text:k,timestamp:l(w.getAttribute("begin")),endtime:l(w.getAttribute("end")),part:!/\s$/.test(k)})}else T.push({text:S.textContent?.trim()||"",timestamp:g,endtime:L,part:!1,lineSynced:!0});const b=C[v],x=E?n[E]:void 0;if(x&&T.length>1&&P.length>0)if(x.syllabus&&x.syllabus.length===T.length)T.forEach((I,w)=>{I.romanizedText=x.syllabus[w].text});else{const w=x.text.split(/\s+/).filter(Boolean),k=[];for(let $=0;$<T.length;$+=1)T[$].part&&k.length>0?k[k.length-1].push($):k.push([$]);const _=/[\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff\uac00-\ud7af]/.test(T.map($=>$.text).join(""));if(w.length===k.length)k.forEach(($,W)=>{T[$[0]].romanizedText=w[W]});else if(w.length===T.length)T.forEach(($,W)=>{$.romanizedText=w[W]});else if(_){let $=0;for(const W of k){const z=T[W[0]],G=(W.map(H=>T[H].text).join("").match(/[\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff\uac00-\ud7afA-Za-z0-9]/g)||[]).length;G>0&&$<w.length&&(z.romanizedText=w.slice($,$+G).join(" "),$+=G)}}}y.push({text:T,background:A.length>0,backgroundText:A,timestamp:g,endtime:L,isWordSynced:P.length>0,alignment:b,songPart:R,translation:E?s[E]:void 0,romanizedText:x?.text,oppositeTurn:b==="end"})}return{lines:y,songwriters:o}}catch(e){return console.error("Failed to parse TTML",e),null}}static convertKPoeLyrics(t){if(!t)return null;let e=null;if(Array.isArray(t?.lyrics)?e=t.lyrics:Array.isArray(t?.data?.lyrics)?e=t.data.lyrics:Array.isArray(t?.data)&&(e=t.data),!e||e.length===0)return null;const i=e.filter(h=>!!h),s=[],n=t.type==="Line"||t.type==="line",r={};t.metadata?.agents&&Object.entries(t.metadata.agents).forEach(([h,c])=>{const l=c.alias||h;r[l]=c.type});const a=i.map(h=>h.element?.singer),o=d.calculateLineAlignments(a,r);for(let h=0;h<i.length;h+=1){const c=i[h],l=d.toMilliseconds(c.time),p=d.toMilliseconds(c.duration),y=o[h],u=typeof c.text=="string"?c.text:"",f=d.toMilliseconds(c.time),C=d.toMilliseconds(c.duration),S=d.toMilliseconds(c.endTime)||f+(C||0);let E=[];Array.isArray(c.syllabus)?E=c.syllabus.filter(x=>!!x):Array.isArray(c.words)&&(E=c.words.filter(x=>!!x));const g=[],L=[];if(!n&&E.length>0)for(const x of E){const I=d.toMilliseconds(x.time,f),w=d.toMilliseconds(x.duration),k=w===0&&E.length===1?S:I+w,_={text:typeof x.text=="string"?x.text:"",part:!!x.part,timestamp:I,endtime:k};x.isBackground?L.push(_):g.push(_)}g.length===0&&u&&g.push({text:u,part:!1,timestamp:f,endtime:S||f,lineSynced:n});const R=g.length>0||L.length>0,{transliteration:T}=c;let A;T&&(A=T.text,Array.isArray(T.syllabus)&&T.syllabus.length===g.length&&T.syllabus.forEach((x,I)=>{g[I].romanizedText=x.text}));const P=c.translation?.text,b={text:g,background:L.length>0,backgroundText:L,oppositeTurn:y==="end"||(Array.isArray(c.element)?c.element.includes("opposite")||c.element.includes("right"):!1),timestamp:f,endtime:l+p,isWordSynced:n?!1:R,alignment:y,songPart:c.element?.songPart,romanizedText:A,translation:P};s.push(b)}return s}static toMilliseconds(t,e=0){const i=Number(t);return!Number.isFinite(i)||Number.isNaN(i)?e:Number.isInteger(i)?Math.max(0,Math.round(i)):Math.round(i*1e3)}firstUpdated(){this.lyricsContainer&&(this.lyricsContainer.addEventListener("wheel",this._boundHandleUserScroll,{passive:!0}),this.lyricsContainer.addEventListener("touchmove",this._boundHandleUserScroll,{passive:!0}))}_onTimeChanged(t,e){const s=Math.abs(e-t)>ri,n=this.findActiveLineIndices(e),r=this.activeLineIndices;if(!d.arraysEqual(n,r)||s){if(this.lyricsContainer){for(const o of r)if(!n.includes(o)){const h=this._getLineElement(o);if(h){s||this.isUserScrolling||d.isLineSyncedLine(this.lyrics?.[o])?d.unfinishSyllables(h):d.finishSyllablesUpToTime(h,e),h.classList.remove("active","bg-expanded","scroll-exiting"),h.classList.contains("pre-active")&&h.classList.remove("pre-active");const c=this.preActiveLineElements.indexOf(h);c!==-1&&this.preActiveLineElements.splice(c,1)}}for(const o of n)if(!r.includes(o)){const h=this._getLineElement(o);if(h){h.classList.add("active"),h.classList.remove("pre-active","scroll-exiting");const c=this.preActiveLineElements.indexOf(h);c!==-1&&this.preActiveLineElements.splice(c,1)}}for(const o of this.preActiveLineElements){const h=d.getLineIndexFromElement(o);(h===null||!n.includes(h)&&o!==this.currentPrimaryActiveLine)&&o.classList.remove("pre-active")}this.preActiveLineElements=this.preActiveLineElements.filter(o=>o.classList.contains("pre-active"))}this.startAnimationFromTime(e)}if(this._handleActiveLineScroll(r,s),this.clearPastLineHighlights(),this.lyricsContainer){for(const l of this.activeLineIndices){const p=this._getLineElement(l);p&&d.updateSyllablesForLine(p,e)}for(const l of this.activeGapLineElements)d.updateSyllablesForLine(l,e);if(this.gapElementCache.size>0)for(const[,l]of this.gapElementCache){const p=l._cachedStartTime??parseFloat(l.getAttribute("data-start-time")||"0"),y=l._cachedEndTime??parseFloat(l.getAttribute("data-end-time")||"0"),u=e>=p&&e<y,f=l.classList.contains("active"),C=l.classList.contains("gap-exiting"),v=pt,S=f&&!C&&e>=y-v;if(u&&(!f||s)&&!C){l.classList.remove("gap-exiting"),s&&f&&(l.classList.remove("active"),l.offsetWidth);const E=y-p,L=d.getGapLoopDelay(E)+(e-p);l.style.setProperty("--gap-loop-delay",`-${L}ms`),l.classList.add("active"),this.activeGapLineElements.includes(l)||this.activeGapLineElements.push(l),l.querySelectorAll(".lyrics-syllable").forEach(T=>{const A=parseFloat(T.getAttribute("data-start-time")||"0"),P=parseFloat(T.getAttribute("data-end-time")||"0");e>P?(T.classList.add("finished"),T.classList.contains("highlight")||d.updateSyllableAnimation(T,e-A)):e>=A&&e<=P&&d.updateSyllableAnimation(T,e-A)})}else if(S){l.classList.remove("active"),l.offsetWidth,l.classList.add("gap-exiting");const E=this.activeGapLineElements.indexOf(l);E!==-1&&this.activeGapLineElements.splice(E,1),setTimeout(()=>{l.classList.remove("gap-exiting")},pt)}else if(!u&&(f||C)){l.classList.remove("active"),l.classList.remove("gap-exiting");const E=this.activeGapLineElements.indexOf(l);E!==-1&&this.activeGapLineElements.splice(E,1)}else C&&e<y-v&&l.classList.remove("gap-exiting")}else this.lyricsContainer&&this.lyricsContainer.querySelectorAll(".lyrics-gap").forEach(p=>{const y=parseFloat(p.getAttribute("data-start-time")||"0"),u=parseFloat(p.getAttribute("data-end-time")||"0"),f=e>=y&&e<u,C=p.classList.contains("active"),v=p.classList.contains("gap-exiting"),S=pt,E=C&&!v&&e>=u-S;if(f&&(!C||s)&&!v){p.classList.remove("gap-exiting"),s&&C&&(p.classList.remove("active"),p.offsetWidth);const g=u-y,R=d.getGapLoopDelay(g)+(e-y);p.style.setProperty("--gap-loop-delay",`-${R}ms`),p.classList.add("active"),this.activeGapLineElements.includes(p)||this.activeGapLineElements.push(p)}else if(E){p.classList.remove("active"),p.offsetWidth,p.classList.add("gap-exiting");const g=this.activeGapLineElements.indexOf(p);g!==-1&&this.activeGapLineElements.splice(g,1),setTimeout(()=>{p.classList.remove("gap-exiting")},pt)}else if(!f&&(C||v)){p.classList.remove("active"),p.classList.remove("gap-exiting");const g=this.activeGapLineElements.indexOf(p);g!==-1&&this.activeGapLineElements.splice(g,1)}else v&&e<u-S&&p.classList.remove("gap-exiting")});const o=this.findInstrumentalGapAt(e);if(o){if(this.lastInstrumentalIndex=o.insertBeforeIndex,o.insertBeforeIndex>0){const l=this._getLineElement(o.insertBeforeIndex-1);l&&l.classList.contains("persist-highlight")&&!l.classList.contains("active")&&d.unfinishSyllables(l)}}else this.lastInstrumentalIndex!==null&&(this.lastInstrumentalIndex=null);const h=this.lyrics&&this.lyrics.length>0?this.lyrics[this.lyrics.length-1]:null,c=this.lyricsContainer.querySelector(".lyrics-footer");if(c&&h&&h.endtime>0){const l=e>h.endtime+200;if(l&&!c.classList.contains("active")){c.classList.add("active");const p=this.lyricsContainer.querySelector(".lyrics-line:last-of-type");if(p){p.classList.remove("pre-active");const y=this.preActiveLineElements.indexOf(p);y!==-1&&this.preActiveLineElements.splice(y,1)}this.autoScroll&&!this.isUserScrolling&&!this.isClickSeeking&&this.focusLine(c)}else!l&&c.classList.contains("active")&&c.classList.remove("active")}}}updated(t){if((t.has("lyrics")||t.has("isLoading")&&!this.isLoading&&!!this.lyrics)&&(this._invalidateCaches(),this._ensureLineDataCache(),this._updateCachedIsUnsynced(),this._updateCharTimingData(),this.lyricsContainer&&this.lyrics)){const i=this.findActiveLineIndices(this.currentTime);for(const r of i){const a=this._getLineElement(r);a&&a.classList.add("active")}const s=this.getPrimaryActiveLineIndex(i);if(this.setBackgroundExpandedLine(s!==null?this._getLineElement(s):null),this._onTimeChanged(0,this.currentTime),this.positionedLineElements.length===0){const r=this.lyricsContainer.querySelector(".lyrics-line");r&&this.updatePositionClasses(r)}this.visibilityObserver?.disconnect(),this.visibilityObserver=new IntersectionObserver(r=>{r.forEach(a=>{a.target.classList.toggle("far-line",!a.isIntersecting)})},{root:this.lyricsContainer,rootMargin:"200px",threshold:0}),this.lyricsContainer.querySelectorAll(".lyrics-line").forEach(r=>this.visibilityObserver.observe(r))}if(t.has("duration")&&this.duration===-1){this.currentTime=0,this.activeLineIndices=[],this.activeMainWordIndices.clear(),this.activeBackgroundWordIndices.clear(),this.mainWordProgress.clear(),this.backgroundWordProgress.clear(),this.mainWordAnimations.clear(),this.backgroundWordAnimations.clear(),this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this.clearBackgroundExpandedLine(),this.setUserScrolling(!1),this.animationFrameId&&(cancelAnimationFrame(this.animationFrameId),this.animationFrameId=void 0),this.userScrollTimeoutId&&(clearTimeout(this.userScrollTimeoutId),this.userScrollTimeoutId=void 0),this.scrollUnlockTimeout&&(clearTimeout(this.scrollUnlockTimeout),this.scrollUnlockTimeout=void 0),this.scrollAnimationTimeout&&(clearTimeout(this.scrollAnimationTimeout),this.scrollAnimationTimeout=void 0),this.lyricsContainer&&(this.lyricsContainer.scrollTop=0);return}(t.has("query")||t.has("musicId")||t.has("isrc")||t.has("ttml")||t.has("songTitle")||t.has("songArtist")||t.has("songAlbum")||t.has("songDurationMs"))&&!t.has("currentTime")&&this.fetchLyrics(),t.has("currentTime")&&this.lyrics}_handleActiveLineScroll(t,e=!1){if(!this.lyricsContainer||!this.lyrics||this.lyrics.length===0)return;if(this.lyricsContainer.querySelector(".lyrics-footer")?.classList.contains("active")){this.setBackgroundExpandedLine(null);return}let s=350,n=-1;for(let c=0;c<this.lyrics.length;c+=1)if(this.lyrics[c].timestamp>this.currentTime){n=c-1;break}if(n===-1&&this.lyrics.length>0&&this.currentTime>=this.lyrics[this.lyrics.length-1].timestamp&&(n=this.lyrics.length-1),n!==-1&&n+1<this.lyrics.length){const c=this.lyrics[n],p=this.lyrics[n+1].timestamp-c.endtime;s=Math.min(500,Math.max(350,p))}const r=this.currentTime+s,a=this.findActiveLineIndices(r);let o=null;if(a.length>0){const c=this.getPrimaryScrollLineIndex(a,r);c!==null&&c!==-1&&(o=this._getLineElement(c))}if(!o){const c=this.getLineIndexAtTime(r,0);c!==null&&c!==-1&&(o=this._getLineElement(c))}if(!o){this.setBackgroundExpandedLine(null);return}const h=s;(o!==this.currentPrimaryActiveLine||e)&&o.style.setProperty("--scroll-duration",`${h}ms`),this.setBackgroundExpandedLine(o),o.classList.contains("active")||(o.classList.add("pre-active"),this.preActiveLineElements.includes(o)||this.preActiveLineElements.push(o)),this.focusLine(o,e,h)}_getTextWidth(t,e){return this._textWidthCanvas||(this._textWidthCanvas=document.createElement("canvas"),this._textWidthCtx=this._textWidthCanvas.getContext("2d",{willReadFrequently:!0})),this._textWidthCtx?(this._textWidthCtx.font=e,this._textWidthCtx.measureText(t).width):0}_rebuildDomCache(){if(!this.lyricsContainer||(this.lineElementCache.clear(),this.gapElementCache.clear(),this.cachedLineArray=[],!this.lyrics))return;for(let e=0;e<this.lyrics.length;e+=1){const i=this.lyricsContainer.querySelector(`#lyrics-line-${e}`);i&&this.lineElementCache.set(e,i);const s=this.lyricsContainer.querySelector(`#gap-${e}`);s&&(s._cachedStartTime=parseFloat(s.getAttribute("data-start-time")||"0"),s._cachedEndTime=parseFloat(s.getAttribute("data-end-time")||"0"),this.gapElementCache.set(e,s))}const t=this.lyricsContainer.querySelectorAll(".lyrics-line");this.cachedLineArray=Array.from(t)}_getLineElement(t){const e=this.lineElementCache.get(t);if(e)return e;if(!this.lyricsContainer)return null;const i=this.lyricsContainer.querySelector(`#lyrics-line-${t}`);return i&&this.lineElementCache.set(t,i),i}_getGapElement(t){const e=this.gapElementCache.get(t);if(e)return e;if(!this.lyricsContainer)return null;const i=this.lyricsContainer.querySelector(`#gap-${t}`);return i&&this.gapElementCache.set(t,i),i}_invalidateCaches(){this.cachedAllGaps=[],this.cachedIsUnsynced=!1,this.cachedLineData=null,this.lineElementCache.clear(),this.gapElementCache.clear(),this.cachedLineArray=[],this.cachedScrollPaddingTop=null,this.preActiveLineElements=[],this.positionedLineElements=[],this.activeGapLineElements=[],this.clearBackgroundExpandedLine(),this.visibilityObserver?.disconnect(),this.visibilityObserver=void 0}_updateCachedIsUnsynced(){this.cachedIsUnsynced=this.lyrics&&this.lyrics.length>0?this.lyrics.every(t=>t.timestamp===0&&t.endtime===0):!1}_ensureLineDataCache(){this.cachedLineData||!this.lyrics||(this.cachedLineData=this.lyrics.map(t=>{const e=[];let i=[];t.text.forEach((f,C)=>{i.push(f);const v=t.text[C+1];(!v||f.part===!1||/\s$/.test(f.text)||v&&f.isBackground!==v.isBackground)&&(e.push(i),i=[])}),i.length>0&&e.push(i);const s=new Array(e.length).fill(!1),n=new Array(e.length).fill(!1),r=new Array(e.length).fill(!1),a=new Array(e.length).fill(!1),o=new Array(e.length).fill(""),h=new Array(e.length).fill(0),c=new Array(e.length).fill(0),l=new Array(e.length).fill(0),p=new Array(e.length).fill(0);let y=!1,u=0;for(;u<e.length;){let f=u;for(;f<e.length-1;){const M=e[f],D=M[M.length-1].text;if(/\s$/.test(D))break;f+=1}const C=e.slice(u,f+1).flatMap(M=>M.map(D=>D.text)).join("").trim(),v=e[u][0].timestamp,S=e[f],E=S[S.length-1].endtime,g=E-v,L=/[\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff\uac00-\ud7af]/.test(C),R=/[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\u0590-\u05FF]/.test(C);R&&(y=!0);const T=C.includes("-"),A=C.length,P=!L&&!R&&!T&&A>0,b=t.isWordSynced===!1||t.text.some(M=>M.lineSynced);let x=P&&A>0&&A<=7;x&&(A<=1?x=g>=1050&&g>=A*525:A<=3?x=g>=pi+(A-2)*140:x=g>=850&&g>=A*190);const I=g>=Math.max(700,A*85),w=A>=2&&A<=3&&g>=Math.max(ui,A*150),k=A>=4&&g>=Math.max(1300,A*260),_=P&&!b&&!x&&(A>=8&&I||A<8&&k),$=P&&!b&&!x&&w,W=x&&!b;let z=0;for(let M=u;M<=f;M+=1){s[M]=x,n[M]=W,r[M]=_,a[M]=$,o[M]=C,h[M]=g,c[M]=z,l[M]=v,p[M]=E;const D=e[M].map(G=>G.text).join("");z+=D.replace(/\s/g,"").length}u=f+1}return{wordGroups:e,groupGrowable:s,groupGlowing:n,groupCharRise:r,groupCharDrag:a,vwFullText:o,vwFullDuration:h,vwCharOffset:c,vwStartMs:l,vwEndMs:p,lineIsRTL:y}}))}_updateCharTimingData(){if(!this.shadowRoot)return;this._rebuildDomCache();const t=this.shadowRoot.querySelector(".lyrics-syllable");if(!t)return;const e=getComputedStyle(t),{font:i}=e,s=Number.parseFloat(e.fontSize)||16,n=Array.from(this.shadowRoot.querySelectorAll(".lyrics-word.growable, .lyrics-word.char-rise, .lyrics-word.char-drag"));if(n.length===0)return;const r=new Map;n.forEach((a,o)=>{const h=a.dataset.virtualWordId||`word-${o}`,c=r.get(h);c?c.push(a):r.set(h,[a])}),r.forEach(a=>{const o=[];a.forEach(g=>{g.querySelectorAll(".lyrics-syllable-wrap").forEach(R=>{const T=R.querySelector(".lyrics-syllable");T&&o.push(T)})});const h=o.flatMap(g=>{const L=Array.from(g.querySelectorAll(".char")),R=g;return R._cachedCharSpans=L,L});if(h.length===0)return;a.forEach(g=>{const L=g;L._cachedVirtualWordElements=a,L._cachedVirtualWordCharSpans=h});const c=o.map(g=>{const L=g._cachedCharSpans,R=L.map(A=>this._getTextWidth(A.textContent||"",i)),T=R.reduce((A,P)=>A+P,0);return{syl:g,spans:L,charWidths:R,totalWidth:T,start:parseFloat(g.getAttribute("data-start-time")||""),end:parseFloat(g.getAttribute("data-end-time")||"")}}),l=c.reduce((g,L)=>g+L.totalWidth,0);if(l<=0)return;const p=Math.min(...c.map(g=>g.start).filter(g=>Number.isFinite(g))),y=Math.max(...c.map(g=>g.end).filter(g=>Number.isFinite(g))),u=y-p,f=Number.isFinite(p)&&Number.isFinite(y)&&u>0,C=f?l/u:0,v=de*Math.max(1,s)/2,S=C>0?v/C:100;let E=0;c.forEach(g=>{let L=0;const R=g.end-g.start,T=f&&Number.isFinite(g.start)&&Number.isFinite(g.end)&&R>0&&g.totalWidth>0;g.spans.forEach((A,P)=>{const b=g.charWidths[P];let x=E/l,I=b/l;if(T){const k=g.start-p+L/g.totalWidth*R,_=b/g.totalWidth*R;x=d.clamp(k/u,0,1),I=d.clamp(_/u,0,1)}const w=A;w.dataset.wipeStart=x.toFixed(4),w.dataset.wipeDuration=I.toFixed(4),w.dataset.preWipeDuration=S.toFixed(2),w.style.setProperty("--word-wipe-width",`${l}px`),w.style.setProperty("--char-wipe-position",`${-E}px`),E+=b,L+=b})})})}static arraysEqual(t,e){return t.length===e.length&&t.every((i,s)=>i===e[s])}static isLineSyncedLine(t){return t?t.isWordSynced===!1||t.text.some(e=>e.lineSynced):!1}getLineHighlightEndTime(t){if(!this.lyrics)return 0;const e=this.lyrics[t];if(!e)return 0;const i=Math.max(e.endtime,e.timestamp),s=this.lyrics[t+1];return!s||s.timestamp<=e.timestamp?i>e.timestamp?i+200:i:i>e.timestamp&&(s.timestamp<i||s.timestamp-i>=wt)?i:s.timestamp}static getLineIndexFromElement(t){if(!t)return null;const e=t.id.match(/^lyrics-line-(\d+)$/);return e?parseInt(e[1],10):null}static getGapLoopDelay(t){const e=Dt,s=((t-pt)%ut+ut)%ut;return((e-s)%ut+ut)%ut}clearPreActiveClasses(t=null){if(!this.lyricsContainer)return;const e=[];for(const i of this.preActiveLineElements)d.getLineIndexFromElement(i)===t?e.push(i):i.classList.remove("pre-active");this.preActiveLineElements=e}setBackgroundExpandedLine(t){const e=t&&!t.classList.contains("lyrics-gap")&&t.querySelector(".background-vocal-container")?t:null;if(this.backgroundExpandedLine===e){e&&!e.classList.contains("bg-expanded")&&e.classList.add("bg-expanded");return}this.backgroundExpandedLine?.classList.remove("bg-expanded"),this.backgroundExpandedLine=e,e?.classList.add("bg-expanded")}clearBackgroundExpandedLine(){this.backgroundExpandedLine?.classList.remove("bg-expanded"),this.backgroundExpandedLine=null}getPrimaryActiveLineIndex(t){if(t.length===0)return null;const e=t[0],i=t[t.length-1];let s=Math.max(e,i-2);const n=d.getLineIndexFromElement(this.currentPrimaryActiveLine);return n!==null&&t.includes(n)&&(t.length<=3||s<n)&&(s=n),s}getPrimaryScrollLineIndex(t,e){if(!this.lyrics||this.lyrics.length===0)return null;const i=this.getLineIndexAtTime(e,this.lastActiveIndex);if(i===-1)return null;const s=d.getLineIndexFromElement(this.currentPrimaryActiveLine);return s!==null&&i>s&&this.lyrics[s]&&this.lyrics[i]&&this.lyrics[s].endtime===this.lyrics[i].endtime&&this.findActiveLineIndices(e).length<=3?s:i}getOverlapClusterForActiveIndices(t,e){if(!this.lyrics||t.length===0)return null;let i=t[0];for(;i>0&&this.lyrics[i-1].endtime>=this.lyrics[i].timestamp;)i-=1;let s=i,n=this.lyrics[i].endtime;for(;s+1<this.lyrics.length&&this.lyrics[s+1].timestamp<=n;)s+=1,n=Math.max(n,this.lyrics[s].endtime);let r=i,a=this.lyrics[i].endtime;for(let o=i;o<=s&&this.lyrics[o].timestamp<=e;o+=1)r=o,a=Math.max(a,this.lyrics[o].endtime);return{start:i,end:s,startedEnd:r,startedEndTime:a}}focusLine(t,e=!1,i=void 0,s=!1,n=!1){const r=t!==this.currentPrimaryActiveLine;if(r&&!n){this.lastPrimaryActiveLine=this.currentPrimaryActiveLine,this.lastPrimaryActiveLine&&(this.lastPrimaryActiveLine.style.setProperty("--scroll-duration",`${i??zt}ms`),this.lastPrimaryActiveLine.classList.add("scroll-exiting")),this.currentPrimaryActiveLine=t,this.currentPrimaryActiveLine.classList.remove("scroll-exiting");const a=d.getLineIndexFromElement(t);a!==null&&(this.lastActiveIndex=a)}(r||e)&&this.updatePositionClasses(t),!s&&(e||r||n)&&this.autoScroll&&!this.isUserScrolling&&!this.isClickSeeking&&this.scrollToActiveLineYouLy(t,e,i)}setUserScrolling(t){this.isUserScrolling=t,t?this.lyricsContainer?.classList.add("user-scrolling"):this.lyricsContainer?.classList.remove("user-scrolling")}handleUserScroll(){this.isProgrammaticScroll||this.isClickSeeking||(this.setUserScrolling(!0),this.clearPastLineHighlights(),this.userScrollTimeoutId&&clearTimeout(this.userScrollTimeoutId),this.userScrollTimeoutId=window.setTimeout(()=>{this.setUserScrolling(!1),this.userScrollTimeoutId=void 0,this.activeLineIndices.length>0&&this._handleActiveLineScroll([],!1)},2e3))}clearPastLineHighlights(){if(!this.lyricsContainer)return;const t=this.cachedLineArray.length?this.cachedLineArray:Array.from(this.lyricsContainer.querySelectorAll(".lyrics-line:not(.lyrics-gap)")),i=this.lyricsContainer.getBoundingClientRect().top+this.getScrollPaddingTop();for(let s=0;s<t.length;s+=1){const n=t[s],r=n.classList.contains("active"),o=n.getBoundingClientRect().bottom<i-2;!r&&o&&d.unfinishSyllables(n)}}getLineIndexAtTime(t,e=0){if(!this.lyrics||this.lyrics.length===0)return-1;const i=this.lyrics.length,s=Math.max(0,Math.min(e,i-1));for(let n=s;n<i;n+=1){const r=this.lyrics[n];if(r.timestamp>t)break;if(t>=r.timestamp&&t<r.endtime)return n}for(let n=s-1;n>=0;n-=1){const r=this.lyrics[n];if(t>=r.timestamp&&t<r.endtime)return n;if(r.endtime<t)break}for(let n=0;n<i;n+=1){const r=this.lyrics[n];if(r.timestamp>t)break;if(t>=r.timestamp&&t<r.endtime)return n}return-1}findActiveLineIndices(t){if(!this.lyrics||this.lyrics.length===0)return[];const e=[];for(let i=0;i<this.lyrics.length;i+=1){const s=this.lyrics[i],n=this.getLineHighlightEndTime(i);if(s.timestamp>t)break;t>=s.timestamp&&t<n&&e.push(i)}return e}findInstrumentalGapAt(t){if(!this.lyrics||this.lyrics.length===0)return null;const e=this.lyrics[0];if(t>=0&&t<e.timestamp){const s=e.timestamp;return s-0>=wt?{insertBeforeIndex:0,gapStart:0,gapEnd:s}:null}for(let i=0;i<this.lyrics.length-1;i+=1){const s=this.lyrics[i],n=this.lyrics[i+1],r=s.endtime,a=n.timestamp;if(t>r&&t<a)return a-r>=wt?{insertBeforeIndex:i+1,gapStart:r,gapEnd:a}:null}return null}findAllInstrumentalGaps(){if(this.cachedAllGaps.length>0)return this.cachedAllGaps;if(!this.lyrics||this.lyrics.length===0)return[];const t=[],e=this.lyrics[0];e.timestamp>=wt&&t.push({insertBeforeIndex:0,gapStart:0,gapEnd:e.timestamp});for(let i=0;i<this.lyrics.length-1;i+=1){const s=this.lyrics[i],n=this.lyrics[i+1],r=s.endtime,a=n.timestamp;a-r>=wt&&t.push({insertBeforeIndex:i+1,gapStart:r,gapEnd:a})}return this.cachedAllGaps=t,t}startAnimationFromTime(t){if(this.animationFrameId&&(cancelAnimationFrame(this.animationFrameId),this.animationFrameId=void 0),!this.lyrics)return;const e=this.findActiveLineIndices(t);if(d.arraysEqual(e,this.activeLineIndices)||(this.activeLineIndices=e),this.activeMainWordIndices.clear(),this.activeBackgroundWordIndices.clear(),this.mainWordAnimations.clear(),this.backgroundWordAnimations.clear(),this.mainWordProgress.clear(),this.backgroundWordProgress.clear(),e.length!==0){for(const i of e){const s=this.lyrics[i];let n=-1;for(let a=0;a<s.text.length;a+=1)if(t>=s.text[a].timestamp&&t<=s.text[a].endtime){n=a;break}this.activeMainWordIndices.set(i,n);let r=-1;if(s.backgroundText){for(let a=0;a<s.backgroundText.length;a+=1)if(t>=s.backgroundText[a].timestamp&&t<=s.backgroundText[a].endtime){r=a;break}}this.activeBackgroundWordIndices.set(i,r)}this.setupAnimations(),this.interpolate&&this.animateProgress()}}updateActiveLineAndWords(){if(!this.lyrics)return;const t=this.findActiveLineIndices(this.currentTime);d.arraysEqual(t,this.activeLineIndices)||(this.activeLineIndices=t),this.activeMainWordIndices.clear(),this.activeBackgroundWordIndices.clear();for(const e of t){const i=this.lyrics[e];let s=-1;for(let r=0;r<i.text.length;r+=1)if(this.currentTime>=i.text[r].timestamp&&this.currentTime<=i.text[r].endtime){s=r;break}this.activeMainWordIndices.set(e,s);let n=-1;if(i.backgroundText){for(let r=0;r<i.backgroundText.length;r+=1)if(this.currentTime>=i.backgroundText[r].timestamp&&this.currentTime<=i.backgroundText[r].endtime){n=r;break}}this.activeBackgroundWordIndices.set(e,n)}}setupAnimations(){if(this.activeLineIndices.length===0||!this.lyrics){this.mainWordAnimations.clear(),this.backgroundWordAnimations.clear();return}for(const t of this.activeLineIndices){const e=this.lyrics[t],i=this.activeMainWordIndices.get(t)??-1,s=this.activeBackgroundWordIndices.get(t)??-1;if(i!==-1){const n=e.text[i],r=n.endtime-n.timestamp,a=this.currentTime-n.timestamp;this.mainWordAnimations.set(t,{startTime:performance.now()-a,duration:r})}else this.mainWordAnimations.set(t,{startTime:0,duration:0});if(s!==-1&&e.backgroundText){const n=e.backgroundText[s],r=n.endtime-n.timestamp,a=this.currentTime-n.timestamp;this.backgroundWordAnimations.set(t,{startTime:performance.now()-a,duration:r})}else this.backgroundWordAnimations.set(t,{startTime:0,duration:0})}}handleLineClick(t){this.lyricsContainer&&(this.lyricsContainer.querySelectorAll(".lyrics-line").forEach(n=>{d.resetSyllables(n),n.classList.remove("scroll-animate","scroll-exiting"),n.style.removeProperty("--scroll-delta"),n.style.removeProperty("--lyrics-line-delay")}),this.lyricsContainer.classList.remove("wheel-scrolling")),this.scrollAnimationState&&(this.scrollAnimationState.isAnimating=!1,this.scrollAnimationState.pendingUpdate=null),this.scrollAnimationTimeout&&(clearTimeout(this.scrollAnimationTimeout),this.scrollAnimationTimeout=void 0),this.userScrollTimeoutId&&(clearTimeout(this.userScrollTimeoutId),this.userScrollTimeoutId=void 0),this.setUserScrolling(!1),this.currentPrimaryActiveLine=null,this.lastPrimaryActiveLine=null,this.activeLineIds.clear(),this.animatingLines=[],this.setBackgroundExpandedLine(null);const e=this.lyricsContainer?.querySelector(`.lyrics-line[data-start-time="${t.text[0]?.timestamp||0}"]`);e&&this.lyricsContainer&&(this.currentPrimaryActiveLine=e,this.currentScrollOffset=-this.lyricsContainer.scrollTop,this.isClickSeeking=!0,this.clickSeekTimeout&&clearTimeout(this.clickSeekTimeout),this.clickSeekTimeout=setTimeout(()=>{this.isClickSeeking=!1},800),this.scrollToActiveLineYouLy(e,!0),this.setBackgroundExpandedLine(e));const i=new CustomEvent("line-click",{detail:{timestamp:t.timestamp},bubbles:!0,composed:!0});this.dispatchEvent(i)}static getBackgroundTextPlacement(t){if(!t.backgroundText||t.backgroundText.length===0||t.text.length===0)return"after";const e=t.text[0].timestamp;return t.backgroundText[0].timestamp<e?"before":"after"}scrollToActiveLine(){if(!this.lyricsContainer||this.activeLineIndices.length===0)return;const t=Math.min(...this.activeLineIndices),e=this.lyricsContainer.querySelector(`.lyrics-line:nth-child(${t+1})`);if(e){const i=this.lyricsContainer.clientHeight,s=e.offsetTop,n=e.clientHeight,r=e.querySelector(".background-text.before");let a=0;r&&(a=r.clientHeight/2);const o=s-i/2+n/2-a;requestAnimationFrame(()=>{this.isProgrammaticScroll=!0,this.lyricsContainer?.scrollTo({top:o,behavior:"smooth"}),setTimeout(()=>{this.isProgrammaticScroll=!1},100)})}}scrollToInstrumental(t){if(!this.lyricsContainer)return;const e=this.lyricsContainer.querySelector(`#gap-${t}`);if(e){const s=this.getScrollPaddingTop()-e.offsetTop;this.isProgrammaticScroll=!0,this.clearPastLineHighlights(),this.animateScrollYouLy(s,!1),setTimeout(()=>{this.isProgrammaticScroll=!1},250)}}getScrollPaddingTop(){if(this.cachedScrollPaddingTop!==null)return this.cachedScrollPaddingTop;if(!this.lyricsContainer)return 0;const e=getComputedStyle(this).getPropertyValue("--lyrics-scroll-padding-top")||"25%";let i;return e.includes("%")?i=this.lyricsContainer.clientHeight*(parseFloat(e)/100):i=parseFloat(e)||0,this.cachedScrollPaddingTop=i,i}animateScrollYouLy(t,e=!1,i=void 0){if(!this.lyricsContainer)return;const s=this.lyricsContainer,n=Math.max(0,-t);this.scrollAnimationState||(this.scrollAnimationState={isAnimating:!1,pendingUpdate:null},this.animatingLines=[]);const r=this.scrollAnimationState;if(r.isAnimating&&!e){const b=r.pendingUpdate===null?null:Math.max(0,-r.pendingUpdate);if(Math.abs(s.scrollTop-n)<2||b!==null&&Math.abs(b-n)<2)return;r.pendingUpdate=t;return}this.scrollAnimationTimeout&&(clearTimeout(this.scrollAnimationTimeout),this.scrollAnimationTimeout=void 0),this.scrollUnlockTimeout&&(clearTimeout(this.scrollUnlockTimeout),this.scrollUnlockTimeout=void 0);const{animatingLines:a}=this,o=-n,c=-s.scrollTop-o;if(this.currentScrollOffset=o,Math.abs(s.scrollTop-n)<1&&Math.abs(c)<1){r.isAnimating=!1,r.pendingUpdate=null;return}if(e){for(const b of a)b.classList.remove("scroll-animate"),b.style.removeProperty("--scroll-delta"),b.style.removeProperty("--lyrics-line-delay"),b.style.removeProperty("--scroll-duration");a.length=0,s.scrollTo({top:n,behavior:"smooth"}),r.isAnimating=!1,r.pendingUpdate=null;return}for(const b of a)b.classList.remove("scroll-animate"),b.style.removeProperty("--scroll-delta"),b.style.removeProperty("--lyrics-line-delay"),b.style.removeProperty("--scroll-duration");if(a.length=0,this.cachedLineArray.length===0){const b=this.lyricsContainer.querySelectorAll(".lyrics-line");this.cachedLineArray=Array.from(b)}const l=this.cachedLineArray,p=this.currentPrimaryActiveLine||this.lastPrimaryActiveLine||l[0];if(!p)return;const y=l.indexOf(p);if(y===-1)return;const u=Math.min(450,i??zt),f=u*.1,C=4,v=20,S=l.length,E=Math.max(0,y-v),g=Math.min(S,y+v);let L=0;const R=[],T=new Map;if(c>=0){let b=0;for(let x=E;x<g;x+=1){const I=l[x],w=x>=y?Math.min(b,C)*f:0;x>=y&&!I.classList.contains("lyrics-gap")&&(b+=1),I.style.setProperty("--scroll-delta",`${c}px`),I.style.setProperty("--lyrics-line-delay",`${w}ms`),T.set(I,w),R.push(I);const k=u+100+w;k>L&&(L=k)}}else{let b=0;for(let x=g-1;x>=E;x-=1){const I=l[x],w=x<=y?Math.min(b,C)*f:0;x<=y&&!I.classList.contains("lyrics-gap")&&(b+=1),I.style.setProperty("--scroll-delta",`${c}px`),I.style.setProperty("--lyrics-line-delay",`${w}ms`),T.set(I,w),R.push(I);const k=u+100+w;k>L&&(L=k)}}for(const b of R){const x=T.get(b)??0;b.style.setProperty("--scroll-duration",`${Math.max(100,L-x)}ms`)}s.offsetHeight;for(const b of R)b.classList.add("scroll-animate"),a.push(b);r.isAnimating=!0;const P=400;this.scrollUnlockTimeout=setTimeout(()=>{if(r.isAnimating=!1,r.pendingUpdate!==null){const b=r.pendingUpdate;r.pendingUpdate=null,this.animateScrollYouLy(b,!1,i)}},P),this.scrollAnimationTimeout=setTimeout(()=>{for(let b=0;b<a.length;b+=1){const x=a[b];x.classList.remove("scroll-animate"),x.style.removeProperty("--scroll-delta"),x.style.removeProperty("--lyrics-line-delay"),x.style.removeProperty("--scroll-duration")}a.length=0,this.scrollAnimationTimeout=void 0},L+50),s.scrollTo({top:n,behavior:"instant"})}updatePositionClasses(t){if(!this.lyricsContainer)return;const e=["lyrics-activest","post-active-line","next-active-line","prev-1","prev-2","prev-3","prev-4","next-1","next-2","next-3","next-4"];for(const n of this.positionedLineElements)n.classList.remove(...e);this.positionedLineElements=[],t.classList.add("lyrics-activest"),this.positionedLineElements.push(t),this.cachedLineArray.length===0&&(this.cachedLineArray=Array.from(this.lyricsContainer.querySelectorAll(".lyrics-line")));const i=this.cachedLineArray,s=i.indexOf(t);if(s!==-1)for(let n=Math.max(0,s-4);n<=Math.min(i.length-1,s+4);n+=1){const r=n-s;if(r!==0){const a=i[n];r===-1?a.classList.add("post-active-line"):r===1?a.classList.add("next-active-line"):r<0?a.classList.add(`prev-${Math.abs(r)}`):a.classList.add(`next-${r}`),this.positionedLineElements.push(a)}}}scrollToActiveLineYouLy(t,e=!1,i=void 0){if(!t||!this.lyricsContainer)return;const s=this.getScrollPaddingTop(),n=s-t.offsetTop,r=this.lyricsContainer.getBoundingClientRect().top;if(!e&&Math.abs(t.getBoundingClientRect().top-r-s)<1)return;if(!e&&!t.classList.contains("lyrics-footer")){const o=this.lyricsContainer,h=o.scrollTop+o.clientHeight>=o.scrollHeight-50,c=Math.max(0,-(s-t.offsetTop));if(h&&c>o.scrollTop-50)return}this.lyricsContainer.classList.remove("not-focused","user-scrolling"),this.isProgrammaticScroll=!0,this.setUserScrolling(!1),this.userScrollTimeoutId&&(clearTimeout(this.userScrollTimeoutId),this.userScrollTimeoutId=void 0),this.clearPastLineHighlights(),setTimeout(()=>{this.isProgrammaticScroll=!1},(i??zt)+160),this.animateScrollYouLy(n,e,i)}static clamp(t,e,i){return Math.min(i,Math.max(e,t))}static getVisibleCharacterCount(t){const e=parseFloat(t.getAttribute("data-word-length")||"");return Number.isFinite(e)&&e>0?e:(t.textContent||"").replace(/\s/g,"").length}static getLongWordWipeScale(t){return t<=6?1:1+d.clamp((t-6)/10,0,1)*hi}static applyWipeShape(t,e){const i=d.clamp((e-6)/10,0,1)*di,s=de+i;t.style.setProperty("--wipe-gradient-width",`${s.toFixed(3)}em`),t.style.setProperty("--wipe-gradient-half",`${(s/2).toFixed(3)}em`)}static ensureWordWipeGeometry(t,e){if(t.length===0)return;const i=Math.max(1,e||t.length);t.forEach((s,n)=>{if(s.style.getPropertyValue("--word-wipe-width")||s.style.setProperty("--word-wipe-width",`${i}ch`),!s.style.getPropertyValue("--char-wipe-position")){const r=Number.parseFloat(s.dataset.wipeStart||`${n/Math.max(1,t.length)}`);s.style.setProperty("--char-wipe-position",`${-(d.clamp(r,0,1)*i)}ch`)}})}static clearPreHighlight(t){const e=t;e.classList.remove("pre-highlight"),e.style.removeProperty("--pre-wipe-duration"),e.style.removeProperty("--pre-wipe-delay"),e.style.animation="",e.querySelectorAll(".pre-wipe-lead").forEach(i=>d.clearPreWipeLead(i))}static clearPreWipeLead(t){t.classList.remove("pre-wipe-lead"),t.style.removeProperty("--pre-wipe-duration"),t.style.removeProperty("--pre-wipe-delay")}static hasTextBoundaryAfter(t){return/\s$/.test(t.textContent||"")}static getSyllableWordIndex(t){const e=d.getWordElementForSyllable(t),i=e?.dataset.virtualWordId;if(i)return`virtual:${i}`;const s=e?.dataset.virtualWordStart,n=e?.dataset.virtualWordEnd;return s||n?`virtual:${s||""}:${n||""}`:t.getAttribute("data-word-index")||t.getAttribute("data-syllable-index")||""}static getNextWordSyllable(t,e){const i=t[e],s=d.getSyllableWordIndex(i),n=i;for(let r=e+1;r<t.length;r+=1){const a=t[r];if(a.classList.contains("transliteration"))continue;return d.getSyllableWordIndex(a)===s||!d.hasTextBoundaryAfter(n)?null:a}return null}static getPreviousNonTransliterationSyllable(t,e){for(let i=e-1;i>=0;i-=1){const s=t[i];if(!s.classList.contains("transliteration"))return s}return null}static getRenderedWordSyllables(t){const e=d.getWordElementForSyllable(t);return d.getCachedVirtualWordElements(e).flatMap(n=>Array.from(n.querySelectorAll(".lyrics-syllable"))).filter(n=>!n.classList.contains("transliteration"))}static getWordElementForSyllable(t){return t.parentElement?.parentElement}static getWordPreWipeKey(t){return d.getWordElementForSyllable(t)?.dataset.virtualWordId||`${t.getAttribute("data-start-time")||""}:${d.getSyllableWordIndex(t)}`}static isPreWipeArmed(t){return d.getWordElementForSyllable(t)?._wordPreWipeKey===d.getWordPreWipeKey(t)}static applyWordPreWipe(t,e,i,s,n){if(d.isPreWipeArmed(t))return;const r=d.getWordElementForSyllable(t),a=d.getCachedVirtualWordElements(r),o=d.getCachedVirtualWordCharSpans(r,[]),h=i-s,c=o.length||e.reduce((u,f)=>u+d.getVisibleCharacterCount(f),0)||d.getVisibleCharacterCount(t);d.ensureWordWipeGeometry(o,c);const l=o[0],y=l?.closest(".lyrics-syllable")||e[0]||t;d.applyWipeShape(y,c),y.style.setProperty("--pre-wipe-duration",`${n}ms`),y.style.setProperty("--pre-wipe-delay",`${-h}ms`),y.classList.add("pre-highlight"),l&&(d.applyWipeShape(l,c),l.style.setProperty("--pre-wipe-duration",`${n}ms`),l.style.setProperty("--pre-wipe-delay",`${-h}ms`),l.classList.add("pre-wipe-lead")),a.forEach(u=>{const f=u;f._wordPreWipeKey=d.getWordPreWipeKey(t)})}static maybePreWipeNextWord(t,e,i,s){const n=t[e];if(n.classList.contains("line-synced")||n.classList.contains("transliteration")||n.closest(".lyrics-gap")||!(n.classList.contains("finished")||i>=s-he))return;const a=d.getNextWordSyllable(t,e);if(!a||a.classList.contains("line-synced")||a.classList.contains("transliteration")||a.closest(".lyrics-gap")||a.classList.contains("highlight")||a.classList.contains("finished"))return;const o=a._cachedStartTime;if(!Number.isFinite(o))return;const h=o-s;if(h>oi||h<-50)return;const c=d.getRenderedWordSyllables(a),l=c.length>0?c:[a],p=d.getWordElementForSyllable(a),u=d.getCachedVirtualWordCharSpans(p,[]).length||l.reduce((v,S)=>v+d.getVisibleCharacterCount(S),0);if(u<=0)return;const f=d.clamp(64+u*9,li,ci),C=Math.max(o-f,s-he);i<C||i>=o||d.applyWordPreWipe(a,l,i,C,f)}static getCachedCharSpans(t){const e=t;return e._cachedCharSpans||(e._cachedCharSpans=Array.from(t.querySelectorAll("span.char"))),e._cachedCharSpans}static getCachedVirtualWordElements(t){if(!t)return[];const e=t;if(e._cachedVirtualWordElements)return e._cachedVirtualWordElements;const{virtualWordId:i}=t.dataset;let s=[t];return i&&t.parentElement&&(s=Array.from(t.parentElement.querySelectorAll(".lyrics-word")).filter(n=>n.dataset.virtualWordId===i)),s.forEach(n=>{const r=n;r._cachedVirtualWordElements=s}),s}static getCachedVirtualWordCharSpans(t,e){if(!t)return e;const i=t;if(i._cachedVirtualWordCharSpans)return i._cachedVirtualWordCharSpans;const s=d.getCachedVirtualWordElements(t),n=s.flatMap(a=>Array.from(a.querySelectorAll("span.char"))),r=n.length>0?n:e;return s.forEach(a=>{const o=a;o._cachedVirtualWordCharSpans=r}),r}static updateSyllableAnimation(t,e=0){if(t.classList.contains("highlight"))return;const{classList:i}=t,s=i.contains("pre-highlight"),n=i.contains("rtl-text"),r=d.getCachedCharSpans(t),o=t.parentElement?.parentElement,h=d.getCachedVirtualWordElements(o),c=d.getCachedVirtualWordCharSpans(o,r),l=o?.classList.contains("growable"),p=o?.classList.contains("char-rise"),y=o?.classList.contains("char-drag"),u=t.getAttribute("data-syllable-index")==="0",f=parseFloat(t.getAttribute("data-start-time")||"0"),C=parseFloat(o?.dataset.virtualWordStart||""),v=u&&(!Number.isFinite(C)||Math.abs(f-C)<.5),S=u,E=t.closest(".lyrics-gap")!==null,g=parseFloat(t.getAttribute("data-duration")||"0")||300,L=parseFloat(t.getAttribute("data-word-duration")||t.getAttribute("data-duration")||"0")||g,R=Number.isFinite(C)?e+(f-C):e,T=Math.max(L,g),A=new Map,P=[];if(l&&v&&c.length>0){const b=L,x=b*.09,I=b*1.5;c.forEach(w=>{const k=w.dataset.matrixScale||"1.1",_=w.dataset.charOffsetX||"0",$=w.dataset.shadowIntensity||"0.6",W=w.dataset.translateYPeak||"-2",z=parseFloat(w.dataset.syllableCharIndex||"0"),M=x*z;A.set(w,`grow-dynamic ${I}ms ease-in-out ${M}ms forwards`),P.push({element:w,property:"--matrix-scale",value:k}),P.push({element:w,property:"--char-offset-x",value:`${_}px`}),P.push({element:w,property:"--shadow-intensity",value:$}),P.push({element:w,property:"--translate-y-peak",value:`${W}px`})})}if(p&&v&&c.length>0){const b=Math.max(L,g),x=b*.09,I=b*1.5;c.forEach(w=>{const k=parseFloat(w.dataset.syllableCharIndex||"0"),_=x*k;A.set(w,`rise-char ${I}ms ease-in-out ${_}ms forwards`)})}if(y&&v&&c.length>0){const b=Math.max(L,g),x=d.clamp(b*.15,64,118),I=d.clamp(b*.82,560,900);c.forEach(w=>{const k=parseFloat(w.dataset.syllableCharIndex||"0"),_=x*k;A.set(w,`drag-char ${I}ms ease ${_}ms forwards`)})}if(r.length>0){const b=c.length||r.length||d.getVisibleCharacterCount(t),x=d.getLongWordWipeScale(b);d.applyWipeShape(t,b),d.ensureWordWipeGeometry(c,b),c.forEach(k=>d.applyWipeShape(k,b));const I=!v&&(!!o?._wordWipeStarted||c.some(k=>k.style.animation.includes("wipe")));let w=r;v?w=c:I&&(w=[]),w.length>0&&h.length>0&&h.forEach(k=>{const _=k;_._wordWipeStarted=!0,_._wordPreWipeKey=void 0}),w.forEach((k,_)=>{const $=parseFloat(k.dataset.wipeStart||"0"),W=parseFloat(k.dataset.wipeDuration||"0"),z=parseFloat(k.dataset.syllableCharIndex||`${_}`),M=k.classList.contains("pre-wipe-lead")||s&&z===0,D=T*$,G=Math.max(0,T-D),H=D-R,rt=Math.min(T*W*x,G),Ot=S&&z===0&&!M;let q="char-wipe";M?q="char-wipe":Ot&&(q="char-start-wipe");const N=A.get(k)||k.style.animation||"",at=[];if(N&&(N.includes("grow-dynamic")||N.includes("rise-char")||N.includes("drag-char"))&&at.push(N.split(",")[0].trim()),z>0&&!M&&H>0&&rt>0){const St=Number.parseFloat(k.dataset.preWipeDuration||"100"),ot=Math.min(St,rt*.9,T*.08,H);ot>=16&&at.push(`char-pre-wipe ${ot}ms linear ${H-ot}ms none`)}if(rt>0){const St=M?"both":"forwards";at.push(`${q} ${rt}ms linear ${H}ms ${St}`)}at.length>0&&A.set(k,at.join(", "))})}else{const b=parseFloat(t.getAttribute("data-wipe-ratio")||"1"),x=d.getVisibleCharacterCount(t),I=d.getLongWordWipeScale(x),w=g*b*I;d.applyWipeShape(t,x);let k="wipe";if(s?k=n?"wipe-from-pre-rtl":"wipe-from-pre":S?k=n?"start-wipe-rtl":"start-wipe":k=n?"wipe-rtl":"wipe",t.classList.contains("line-synced"))return;const _=E?"fade-gap":k;t.style.animation=`${_} ${w}ms ${E?"ease-out":"linear"} ${-e}ms forwards`}h.length>0&&h.forEach(b=>{const x=b;x._wordPreWipeKey=void 0}),i.remove("pre-highlight"),i.add("highlight"),c.forEach(b=>d.clearPreWipeLead(b));for(const b of P)b.element.style.setProperty(b.property,b.value);for(const[b,x]of A.entries())b.style.willChange="transform",b.style.removeProperty("background-color"),b.style.animation=x}static resetSyllable(t){if(!t)return;t.style.animation="",t.style.removeProperty("--pre-wipe-duration"),t.style.removeProperty("--pre-wipe-delay"),t.style.transition="none",t.style.backgroundColor="var(--lyplus-text-secondary)";const e=t.querySelectorAll("span.char");for(let i=0;i<e.length;i+=1){const s=e[i];s.style.animation="",s.style.transition="none",s.style.backgroundColor="var(--lyplus-text-secondary)",d.clearPreWipeLead(s)}t.classList.remove("highlight","finished","pre-highlight","cleanup")}static resetWordAnimationState(t){t.querySelectorAll(".lyrics-word").forEach(i=>{const s=i;s._wordPreWipeKey=void 0,s._wordWipeStarted=!1})}static resetSyllables(t){if(!t)return;t.classList.remove("persist-highlight"),d.resetWordAnimationState(t),t._cachedSyllableElements=null;const e=t.getElementsByClassName("lyrics-syllable");for(let i=0;i<e.length;i+=1)d.resetSyllable(e[i]);requestAnimationFrame(()=>{for(let i=0;i<e.length;i+=1){const s=e[i];s.style.removeProperty("background-color"),s.style.removeProperty("transition");const n=s.querySelectorAll("span.char");for(let r=0;r<n.length;r+=1){const a=n[r];a.style.removeProperty("background-color"),a.style.removeProperty("transition"),a.style.removeProperty("will-change")}}})}static unfinishSyllables(t){if(!t)return;t.classList.remove("persist-highlight"),d.resetWordAnimationState(t);const e=t.getElementsByClassName("lyrics-syllable");for(let i=0;i<e.length;i+=1){const s=e[i];s.classList.remove("highlight","finished","pre-highlight","cleanup"),s.style.animation="",s.style.removeProperty("--pre-wipe-duration"),s.style.removeProperty("--pre-wipe-delay"),s.style.removeProperty("background-color"),s.style.removeProperty("transition");const n=s.querySelectorAll("span.char");for(let r=0;r<n.length;r+=1){const a=n[r];a.style.animation="",a.style.removeProperty("will-change"),a.style.removeProperty("background-color"),a.style.removeProperty("transition"),a.style.removeProperty("filter"),d.clearPreWipeLead(a)}}}static finishSyllablesUpToTime(t,e){if(!t)return;let i=!1,s=t._cachedSyllableElements;if(!s){s=Array.from(t.querySelectorAll(".lyrics-syllable"));for(let n=0;n<s.length;n+=1){const r=s[n];r._cachedStartTime=parseFloat(r.getAttribute("data-start-time")||"0"),r._cachedEndTime=parseFloat(r.getAttribute("data-end-time")||"0")}t._cachedSyllableElements=s}for(let n=0;n<s.length;n+=1){const r=s[n],a=r._cachedStartTime;if(Number.isFinite(a)&&e>=a){const{classList:o}=r;o.contains("finished")||(o.contains("highlight")||d.updateSyllableAnimation(r,Math.max(0,e-a)),o.add("finished")),i=!0,o.remove("highlight"),o.remove("pre-highlight"),o.add("cleanup"),r.style.animation="",r.style.removeProperty("--pre-wipe-duration"),r.style.removeProperty("--pre-wipe-delay"),r.style.removeProperty("background-color"),d.applyWipeShape(r,d.getVisibleCharacterCount(r));const h=r.querySelectorAll("span.char");for(let c=0;c<h.length;c+=1){const l=h[c],p=l.style.animation||"";if(p.includes("grow-dynamic")||p.includes("rise-char")||p.includes("drag-char")){const u=p.split(",").map(f=>f.trim()).find(f=>f.includes("grow-dynamic")||f.includes("rise-char")||f.includes("drag-char"));l.style.animation=u||""}else l.style.animation="";l.style.backgroundColor="var(--lyplus-text-primary)",d.clearPreWipeLead(l)}}}i?t.classList.add("persist-highlight"):t.classList.remove("persist-highlight")}static updateSyllablesForLine(t,e){let i=t._cachedSyllableElements;if(!i){i=Array.from(t.querySelectorAll(".lyrics-syllable"));for(let s=0;s<i.length;s+=1){const n=i[s];n._cachedStartTime=parseFloat(n.getAttribute("data-start-time")||"0"),n._cachedEndTime=parseFloat(n.getAttribute("data-end-time")||"0")}t._cachedSyllableElements=i}for(let s=0;s<i.length;s+=1){const n=i[s],r=n._cachedStartTime,a=n._cachedEndTime;if(Number.isFinite(r)&&Number.isFinite(a)){const{classList:o}=n,h=o.contains("highlight"),c=o.contains("finished"),l=o.contains("pre-highlight"),p=h||c||l;if(!(e<r-1e3&&!p)){let y=!1;if(l&&e<r){const u=d.getPreviousNonTransliterationSyllable(i,s);u?.classList.contains("highlight")||u?.classList.contains("finished")||(d.clearPreHighlight(n),y=!0)}y||(e>=r&&e<=a?(h||d.updateSyllableAnimation(n,e-r),c&&o.remove("finished")):e>a?c||(h||d.updateSyllableAnimation(n,e-r),o.add("finished")):(h||c)&&d.resetSyllable(n),d.maybePreWipeNextWord(i,s,e,a))}}}}animateProgress(){const t=performance.now();let e=!1;if(!this.lyrics||this.activeLineIndices.length===0){this.animationFrameId&&(cancelAnimationFrame(this.animationFrameId),this.animationFrameId=void 0);return}for(const i of this.activeLineIndices){const s=this.lyrics[i],n=this.mainWordAnimations.get(i);if(n&&n.duration>0){const a=t-n.startTime;if(a>=0){const o=Math.min(1,a/n.duration);if(this.mainWordProgress.set(i,o),o<1)e=!0;else{const h=this.activeMainWordIndices.get(i)??-1,c=h+1;if(h!==-1&&c<s.text.length){const l=s.text[h],p=s.text[c];this.activeMainWordIndices.set(i,c);const y=p.timestamp-l.endtime,u=p.endtime-p.timestamp;this.mainWordAnimations.set(i,{startTime:performance.now()+y,duration:u}),e=!0}else this.mainWordAnimations.set(i,{startTime:0,duration:0})}}else this.mainWordProgress.set(i,0),e=!0}const r=this.backgroundWordAnimations.get(i);if(r&&r.duration>0){const a=t-r.startTime;if(a>=0){const o=Math.min(1,a/r.duration);if(this.backgroundWordProgress.set(i,o),o<1)e=!0;else{const h=this.activeBackgroundWordIndices.get(i)??-1;if(s.backgroundText&&h!==-1&&h<s.backgroundText.length-1){const c=h+1,l=s.backgroundText[h],p=s.backgroundText[c];this.activeBackgroundWordIndices.set(i,c);const y=p.timestamp-l.endtime,u=p.endtime-p.timestamp;this.backgroundWordAnimations.set(i,{startTime:performance.now()+y,duration:u}),e=!0}else this.backgroundWordAnimations.set(i,{startTime:0,duration:0})}}else this.backgroundWordProgress.set(i,0),e=!0}}e?this.animationFrameId=requestAnimationFrame(this._boundAnimateProgress):this.animationFrameId&&(cancelAnimationFrame(this.animationFrameId),this.animationFrameId=void 0)}generateLRC(){if(!this.lyrics)return"";let t="";this.songTitle&&(t+=`[ti:${this.songTitle}]
`),this.songArtist&&(t+=`[ar:${this.songArtist}]
`),this.songAlbum&&(t+=`[al:${this.songAlbum}]
`),this.lyricsSource&&(t+=`[re:${this.lyricsSource}]
`);for(const e of this.lyrics)if(e.text&&e.text.length>0){const i=d.formatTimestampLRC(e.timestamp),s=e.text.map(n=>n.text).join("").trim();t+=`[${i}]${s}
`}return t}generateTTML(){if(!this.lyrics)return"";let t=`<?xml version="1.0" encoding="UTF-8"?>
`;t+=`<tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyrics">
`,t+=`  <body>
`;let e;for(let i=0;i<this.lyrics.length;i+=1){const s=this.lyrics[i],n=s.songPart;(n!==e||i===0)&&(i>0&&(t+=`    </div>
`),e=n,e?t+=`    <div itunes:song-part="${e}">
`:t+=`    <div>
`);const r=d.formatTimestampTTML(s.timestamp),a=d.formatTimestampTTML(s.endtime);t+=`      <p begin="${r}" end="${a}">
`;for(const o of s.text){const h=d.formatTimestampTTML(o.timestamp),c=d.formatTimestampTTML(o.endtime),l=o.text.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");t+=`        <span begin="${h}" end="${c}">${l}</span>
`}t+=`      </p>
`}return this.lyrics.length>0&&(t+=`    </div>
`),t+=`  </body>
`,t+="</tt>",t}static formatTimestampLRC(t){const e=t/1e3,i=Math.floor(e/60),s=Math.floor(e%60),n=Math.floor(t%1e3/10),r=a=>a.toString().padStart(2,"0");return`${r(i)}:${r(s)}.${r(n)}`}static formatTimestampTTML(t){const e=t/1e3,i=Math.floor(e/3600),s=Math.floor(e%3600/60),n=Math.floor(e%60),r=Math.floor(t%1e3),a=(o,h=2)=>o.toString().padStart(h,"0");return`${a(i)}:${a(s)}:${a(n)}.${a(r,3)}`}downloadLyrics(){if(!this.lyrics||this.lyrics.length===0)return;const t=this.lyrics.some(h=>h.isWordSynced!==!1);let e="",i=this.downloadFormat;i==="auto"&&(i=t?"ttml":"lrc");let s="";if(i==="ttml"?(e=this.generateTTML(),s="application/xml"):(e=this.generateLRC(),s="text/plain"),!e)return;const n=new Blob([e],{type:s}),r=URL.createObjectURL(n),a=document.createElement("a");a.href=r;const o=this.songTitle?`${this.songTitle}${this.songArtist?` - ${this.songArtist}`:""}.${i}`:`lyrics.${i}`;a.download=o,document.body.appendChild(a),a.click(),document.body.removeChild(a),URL.revokeObjectURL(r)}render(){this.fontFamily&&(this.style.fontFamily=this.fontFamily),this.style.setProperty("--highlight-color",this.highlightColor);const t=this.lyricsSource??"Unavailable",e=this.cachedIsUnsynced,i=()=>{if(this.isLoading)return O`
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
        `;if(!this.lyrics||this.lyrics.length===0)return O`<div class="no-lyrics">No lyrics found.</div>`;const s=this.findAllInstrumentalGaps(),n=new Map(s.map(r=>[r.insertBeforeIndex,r]));return this.lyrics.map((r,a)=>{const o=`lyrics-line-${a}`,h=r.text[0]?.timestamp||0,c=r.text[r.text.length-1]?.endtime||0,l=r.backgroundText&&r.backgroundText.length>0,p=l?O`<p class="background-vocal-container">
              <span class="background-vocal-wrap">
                ${r.backgroundText.map(($,W)=>{const z=$.timestamp,M=$.endtime,D=M-z,G=this.showRomanization&&$.romanizedText&&$.romanizedText.trim()!==$.text.trim()?O`<span
                          class="lyrics-syllable transliteration no-chars ${$.lineSynced?"line-synced":""}"
                          data-start-time="${z}"
                          data-end-time="${M}"
                          data-duration="${D}"
                          data-syllable-index="0"
                          data-wipe-ratio="1"
                          >${$.romanizedText}</span
                        >`:"";return O`<span class="lyrics-word"
                    ><span
                      class="lyrics-syllable-wrap${G?" has-transliteration":""}"
                      ><span
                        class="lyrics-syllable no-chars${$.lineSynced?" line-synced":""}"
                        data-start-time="${z}"
                        data-end-time="${M}"
                        data-duration="${D}"
                        data-syllable-index="${W}"
                        data-word-index="${W}"
                        data-word-length="${$.text.replace(/\s/g,"").length}"
                        data-wipe-ratio="1"
                        >${$.text}</span
                      >${G}</span
                    ></span
                  >`})}
              </span>
            </p>`:"",y=l?d.getBackgroundTextPlacement(r):"after",u=this.cachedLineData?.[a],f=u?.wordGroups??[],C=u?.groupGrowable??[],v=u?.groupGlowing??[],S=u?.groupCharRise??[],E=u?.groupCharDrag??[],g=u?.vwFullText??[],L=u?.vwFullDuration??[],R=u?.vwCharOffset??[],T=u?.vwStartMs??[],A=u?.vwEndMs??[],P=u?.lineIsRTL??!1,b=O`<p
          class="main-vocal-container ${P?"rtl-text":""}"
        >
          ${f.map(($,W)=>{const z=C[W],M=v[W],D=S[W],G=E[W],H=z||D||G,rt=$.some(j=>j.lineSynced),Ot=H?g[W]:"",q=H?L[W]:0,N=Ot.replace(/\s/g,"").length,at=H?R[W]:0,St=`${a}:${T[W]}:${A[W]}`,ot=T[W],gi=A[W];let me=0;const Ut=$.map(j=>j.text).join(""),fi=Ut.replace(/\s/g,"").length,yi=Ut.trim().length>=16||/[\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff\uac00-\ud7af]/.test(Ut),bi=$[0].timestamp,vi=$[$.length-1].endtime-bi,xi=Math.max(1.2,Math.min(2.5,1.2+vi/1e3*.6));return O`<span
              class="lyrics-word${z?" growable":""}${D?" char-rise":""}${G?" char-drag":""}${M?" glowing":""}${yi?" allow-break":""}"
              data-virtual-word-id="${St}"
              data-virtual-word-start="${ot}"
              data-virtual-word-end="${gi}"
              style="--rise-duration: ${xi}s"
              >${$.map((j,wi)=>{const Lt=j.timestamp,Nt=j.endtime,Ct=Nt-Lt,Bt=j.text||"",ge=this.showRomanization&&j.romanizedText&&j.romanizedText.trim()!==j.text.trim()?O`<span
                        class="lyrics-syllable transliteration no-chars ${rt?"line-synced":""}"
                        data-start-time="${Lt}"
                        data-end-time="${Nt}"
                        data-duration="${Ct}"
                        data-syllable-index="0"
                        data-wipe-ratio="1"
                        >${j.romanizedText}</span
                      >`:"";let fe=Bt;if(H){const ye=Bt.replace(/\s/g,"").length||1,be=q>0&&Number.isFinite(ot),Si=be?d.clamp((Lt-ot)/q,0,1):0,ve=be?d.clamp(Ct/q,0,1):1;let xe=0;fe=O`${Bt.split("").map(we=>{if(we===" ")return" ";const kt=at+me,ki=xe,Se=Math.max(1,N),Ti=d.clamp(Si+ki/ye*ve,0,1),Ei=ve/ye||1/Se;me+=1,xe+=1;const ke=400,$i=Math.min(1,Math.max(0,(q-ke)/(3e3-ke)))**3,Te=N>5,Gt=q<1200;let Ee=0;if(Te||Gt){let Wt=0;Te&&(Wt+=Math.min((N-5)/5,1)*.4),Gt&&N>3?Wt+=Math.max(0,1-(q-800)/400)*.3:Gt&&N<=3&&(Wt+=Math.max(0,1-(q-800)/400)*.1),Ee=Math.min(Wt,.7)}const Ai=1-(N>1?kt/(N-1):0)*Ee,$e=$i*Ai,It=1+(N<=3?.05:.04)+$e*.08,Li=Math.min(1.1,q/1500);let qt=1;N<=3?qt=.85:N>=6&&(qt=1.1);const Ci=Li*qt,Ii=M?(.35+$e*.45)*Ci:0,Wi=(It-1)/.1,Pi=(q+Ct*2)/3,Mi=Math.min(1,Math.max(.3,Pi/2e3)),_i=-Wi*(2*Mi),Ae=((kt+.5)/N-.5)*2*((It-1)*25),Le=G;let Ht=_i;D?Ht=0:Le&&(Ht=-.78);let jt=Ae;return(D||Le)&&(jt=0),O`<span
                      class="char"
                      data-char-index="${kt}"
                      data-syllable-char-index="${kt}"
                      data-wipe-start="${Ti.toFixed(4)}"
                      data-wipe-duration="${Ei.toFixed(4)}"
                      data-horizontal-offset="${Ae.toFixed(2)}"
                      data-max-scale="${It.toFixed(3)}"
                      data-matrix-scale="${(It*.98).toFixed(3)}"
                      data-char-offset-x="${(jt*.98).toFixed(2)}"
                      data-shadow-intensity="${Ii.toFixed(3)}"
                      data-translate-y-peak="${Ht.toFixed(3)}"
                      style="--word-wipe-width: ${Se}ch; --char-wipe-position: -${kt}ch"
                      >${we}</span
                    >`})}`}return O`<span
                  class="lyrics-syllable-wrap${ge?" has-transliteration":""}"
                  ><span
                    class="lyrics-syllable${rt?" line-synced":""}${H?" has-chars":" no-chars"}"
                    data-start-time="${Lt}"
                    data-end-time="${Nt}"
                    data-duration="${Ct}"
                    data-word-duration="${q}"
                    data-syllable-index="${wi}"
                    data-word-index="${W}"
                    data-word-length="${fi}"
                    data-wipe-ratio="1"
                    >${fe}</span
                  >${ge}</span
                >`})}</span
            >`})}
        </p>`,x=r.text.map($=>$.text).join("").trim(),I=this.showTranslation&&r.translation&&r.translation.trim()!==x?O`<div class="lyrics-translation-container">
                ${r.translation}
              </div>`:"",w=this.showRomanization&&r.romanizedText&&!r.text.some($=>$.romanizedText)&&r.romanizedText.trim()!==x?O`<div
                class="lyrics-romanization-container ${P?"rtl-text":""}"
              >
                ${r.romanizedText}
              </div>`:"";let k=null;const _=n.get(a);if(_){const $=_.gapEnd-_.gapStart,W=$/3,z=d.getGapLoopDelay($);k=O`<div
            id="gap-${a}"
            class="lyrics-line lyrics-gap"
            data-start-time="${_.gapStart}"
            data-end-time="${_.gapEnd}"
            style="--gap-pulse-duration: ${Dt}ms; --gap-loop-delay: -${z}ms; --gap-exit-duration: ${pt}ms; --gap-exit-scale: ${ai};"
          >
            <p class="main-vocal-container">
              <span class="lyrics-word"
                ><span class="lyrics-syllable-wrap"
                  ><span
                    class="lyrics-syllable"
                    data-start-time="${_.gapStart}"
                    data-end-time="${_.gapStart+W}"
                    data-duration="${W}"
                    data-wipe-ratio="1"
                    data-syllable-index="0"
                  ></span></span
                ><span class="lyrics-syllable-wrap"
                  ><span
                    class="lyrics-syllable"
                    data-start-time="${_.gapStart+W}"
                    data-end-time="${_.gapStart+W*2}"
                    data-duration="${W}"
                    data-wipe-ratio="1"
                    data-syllable-index="1"
                  ></span></span
                ><span class="lyrics-syllable-wrap"
                  ><span
                    class="lyrics-syllable"
                    data-start-time="${_.gapStart+W*2}"
                    data-end-time="${_.gapEnd}"
                    data-duration="${W}"
                    data-wipe-ratio="1"
                    data-syllable-index="2"
                  ></span></span
              ></span>
            </p>
          </div>`}return O`
          ${k}
          <div
            id="${o}"
            class="lyrics-line ${r.alignment==="end"?"singer-right":"singer-left"} ${P?"rtl-text":""}"
            data-start-time="${h}"
            data-end-time="${c}"
            @click=${()=>this.handleLineClick(r)}
            tabindex="0"
            @keydown=${$=>{($.key==="Enter"||$.key===" ")&&this.handleLineClick(r)}}
          >
            <div class="lyrics-line-container ${P?"rtl-text":""}">
              ${y==="before"?p:""}
              ${b}
              ${y==="after"?p:""}
              ${w} ${I}
            </div>
          </div>
        `})};return O`
      <div
        class="lyrics-container ${e?"is-unsynced":"blur-inactive-enabled"}"
      >
        ${!this.isLoading&&this.lyrics&&this.lyrics.length>0?O`
              <div class="lyrics-header">
                <div class="header-controls">
                  <button
                    class="download-button ${this.showRomanization?"active":""}"
                    @click=${this.toggleRomanization}
                    title="Toggle Romanization"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      class="lucide lucide-speech-icon lucide-speech"
                    >
                      <path
                        d="M8.8 20v-4.1l1.9.2a2.3 2.3 0 0 0 2.164-2.1V8.3A5.37 5.37 0 0 0 2 8.25c0 2.8.656 3.054 1 4.55a5.77 5.77 0 0 1 .029 2.758L2 20"
                      />
                      <path d="M19.8 17.8a7.5 7.5 0 0 0 .003-10.603" />
                      <path d="M17 15a3.5 3.5 0 0 0-.025-4.975" />
                    </svg>
                  </button>
                  <button
                    class="download-button ${this.showTranslation?"active":""}"
                    @click=${this.toggleTranslation}
                    title="Toggle Translation"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      class="lucide lucide-languages-icon lucide-languages"
                    >
                      <path d="m5 8 6 6" />
                      <path d="m4 14 6-6 2-3" />
                      <path d="M2 5h12" />
                      <path d="M7 2h1" />
                      <path d="m22 22-5-10-5 10" />
                      <path d="M14 18h6" />
                    </svg>
                  </button>
                </div>
                <div class="download-controls">
                  <select
                    class="format-select"
                    @change=${s=>{this.downloadFormat=s.target.value}}
                    .value=${this.downloadFormat}
                    @click=${s=>s.stopPropagation()}
                  >
                    <option value="auto">Auto</option>
                    <option value="lrc">LRC</option>
                    <option value="ttml">TTML</option>
                  </select>
                  <button
                    class="download-button"
                    @click=${this.downloadLyrics}
                    title="Download Lyrics"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      class="lucide lucide-download-icon lucide-download"
                    >
                      <path d="M12 15V3" />
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <path d="m7 10 5 5 5-5" />
                    </svg>
                  </button>
                </div>
              </div>
            `:""}
        ${i()}
        ${this.isLoading?"":O`
              <footer class="lyrics-footer lyrics-line">
                <div class="footer-content">
                  <span
                    class="source-info"
                    style="display: flex; align-items: center; gap: 8px;"
                  >
                    <b style="font-weight: 750;">Source</b> ${t}
                    ${this.availableSources&&this.availableSources.length>1||!this.hasFetchedAllProviders?O`
                          <button
                            class="download-button source-switch-btn"
                            title="Switch Lyrics Source"
                            @click=${this.switchSource}
                            ?disabled=${this.isFetchingAlternatives}
                          >
                            <svg
                              class="source-switch-svg lucide lucide-arrow-down-up-icon lucide-arrow-down-up ${this.isFetchingAlternatives?"is-loading":""}"
                              xmlns="http://www.w3.org/2000/svg"
                              width="12"
                              height="12"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              stroke-width="2"
                              stroke-linecap="round"
                              stroke-linejoin="round"
                            >
                              ${this.isFetchingAlternatives?ae`<path
                                    d="M21 12a9 9 0 1 1-6.219-8.56"
                                  ></path>`:ae`<path d="m3 16 4 4 4-4"></path
                                    ><path d="M7 20V4"></path
                                    ><path d="m21 8-4-4-4 4"></path
                                    ><path d="M17 4v16"></path>`}
                            </svg>
                            <span class="source-switch-label"
                              >${this.isFetchingAlternatives?"Switching...":"Switch"}</span
                            >
                          </button>
                        `:""}
                  </span>
                  ${this.songwriters?O`<span
                        class="songwriters-info"
                        style="margin-top: 4px; font-weight: normal; font-size: 0.9em;"
                      >
                        <b style="font-weight: 750;">Songwriters</b> ${this.songwriters}
                      </span>`:""}
                  <span class="version-info" style="margin-top: 8px;">
                    <b style="font-weight: 750;">am-lyrics</b> v${ce} •

                    <a
                      href="https://github.com/uimaxbai/apple-music-web-components"
                      target="_blank"
                      rel="noopener noreferrer"
                      style="display: inline-flex; align-items: center; gap: 4px;"
                      >Star me on GitHub
                    </a>
                  </span>
                </div>
              </footer>
            `}
      </div>
    `}}return d.styles=Me`
    :host {
      --lyplus-lyrics-palette: var(
        --am-lyrics-highlight-color,
        var(--highlight-color, #ffffff)
      );
      --lyplus-text-primary: var(--lyplus-lyrics-palette);
      /* Use color-mix with the text color rather than just opacity so it adapts */
      --lyplus-text-secondary: color-mix(
        in srgb,
        var(--lyplus-lyrics-palette),
        transparent 45%
      );

      --lyplus-padding-base: 1em;
      --lyplus-padding-line: 10px;
      --lyplus-padding-gap: 0.3em;
      --lyplus-border-radius-base: 0.6em;
      --lyplus-gap-dot-size: 0.4em;
      --lyplus-gap-dot-margin: 0.08em;

      --lyplus-font-size-base: 32px;
      --lyplus-font-size-base-grow: 24.5;
      --lyplus-font-size-subtext: 0.6em;
      --char-rise-y: calc(-0.035 * var(--lyplus-font-size-base));

      --lyplus-blur-amount: 0.07em;
      --lyplus-blur-amount-near: 0.035em;
      --lyplus-fade-gap-timing-function: ease-out;
      --wipe-gradient-width: 0.75em;
      --wipe-gradient-half: 0.375em;

      --lyrics-scroll-padding-top: 25%;

      display: block;
      font-family:
        -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu,
        Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
      background: transparent;
      height: 100%;
      overflow: hidden;
      font-weight: bold;
      color: var(--lyplus-text-primary);
    }

    /* ==========================================================================
       CONTAINER & SCROLL BEHAVIOR
       ========================================================================== */
    .lyrics-container {
      padding: 20px;
      padding-top: 80px;
      border-radius: 8px;
      background-color: transparent;
      width: 100%;
      height: 100%;
      max-height: 100vh;
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      -webkit-touch-callout: none;
      -webkit-user-select: none;
      user-select: none;
      box-sizing: border-box;
      scrollbar-width: none;
      overflow-anchor: none;
    }

    .lyrics-container::-webkit-scrollbar {
      display: none;
    }

    /* Disable transitions during touch-scrolling for 1:1 feedback */
    .lyrics-container.touch-scrolling .lyrics-line,
    .lyrics-container.touch-scrolling .lyrics-plus-metadata {
      transition: none !important;
      filter: none !important;
    }

    /* Apply smooth gliding transition for mouse-wheel scrolling */
    .lyrics-container.wheel-scrolling .lyrics-line {
      transition: transform 0.3s ease-out !important;
      filter: none !important;
    }

    .lyrics-line.scroll-animate {
      /* Preserve the graceful fade duration; the keyframe handles the
         transform, so we only need to keep opacity/filter transitions
         alive without !important overriding the base rule. */
      transition:
        opacity 0.7s ease,
        filter 0.7s ease,
        transform 0.4s cubic-bezier(0.41, 0, 0.12, 0.99)
          var(--lyrics-line-delay, 0ms);
      animation-name: lyrics-scroll;
      animation-duration: var(--scroll-duration, 400ms);
      animation-timing-function: cubic-bezier(0.41, 0, 0.12, 0.99);
      animation-fill-mode: both;
      animation-delay: var(--lyrics-line-delay, 0ms);
    }

    .lyrics-container.user-scrolling .lyrics-line {
      --lyrics-line-delay: 0ms !important;
      transition-delay: 0ms !important;
    }

    /* ==========================================================================
       LYRICS LINE BASE STYLES
       ========================================================================== */
    .lyrics-line {
      padding: var(--lyplus-padding-line);
      opacity: 0.8;
      color: var(--lyplus-text-secondary);
      font-size: var(--lyplus-font-size-base);
      cursor: pointer;
      transform-origin: left;
      /* Graceful 0.7 s fade so the line stays mostly bright while the
         0.4 s scroll animation runs, then settles into the inactive state. */
      transition:
        opacity 0.7s ease,
        transform 0.4s cubic-bezier(0.41, 0, 0.12, 0.99)
          var(--lyrics-line-delay, 0ms),
        filter 0.7s ease;
      content-visibility: auto;
      contain: layout style;
      text-rendering: optimizeLegibility;
    }

    .lyrics-line:not(.scroll-animate) {
      animation: none;
    }

    /* --- Line Container & Vocal Containers --- */
    .lyrics-line-container {
      overflow-wrap: break-word;
      transform-origin: left;
      transform: translateZ(0);
      transition:
        transform 0.7s ease,
        background-color 0.7s,
        color 0.7s;
    }

    .lyrics-line.active .lyrics-line-container,
    .lyrics-line.pre-active .lyrics-line-container {
      transform: translateZ(0);
      transition:
        transform 0.5s ease,
        background-color 0.18s,
        color 0.18s;
    }

    .main-vocal-container {
      transform-origin: 5% 50%;
      margin: 0;
    }

    .background-vocal-container {
      max-height: 0;
      overflow: hidden;
      opacity: 0;
      font-size: var(--lyplus-font-size-subtext);
      line-height: 1.15;
      color: color-mix(in srgb, var(--lyplus-text-secondary) 80%, transparent);
      transition:
        max-height var(--scroll-duration, 400ms)
          cubic-bezier(0.41, 0, 0.12, 0.99),
        opacity var(--scroll-duration, 400ms) cubic-bezier(0.41, 0, 0.12, 0.99);
      margin: 0;
      pointer-events: none;
    }

    .background-vocal-wrap {
      display: block;
      padding-top: 0;
      padding-bottom: 0;
      transition: padding-top var(--scroll-duration, 400ms)
        cubic-bezier(0.41, 0, 0.12, 0.99);
    }

    .lyrics-line.singer-right .background-vocal-container,
    .lyrics-line.rtl-text .background-vocal-container {
      margin-left: auto;
      margin-right: 0;
    }

    /* Background vocals expand only when .bg-expanded is present.
       This is separate from .active so bg vocals can collapse immediately
       while .active stays to keep text white until the scroll passes. */
    .lyrics-line.bg-expanded .background-vocal-container {
      max-height: 4em;
      opacity: 1;
      will-change: opacity;
    }

    .lyrics-line.bg-expanded .background-vocal-wrap {
      padding-top: 0.26em;
    }

    /* --- Line States & Modifiers --- */
    .lyrics-line.active {
      opacity: 1;
      color: var(--lyplus-text-primary);
    }

    .lyrics-line.pre-active {
      opacity: 1;
    }

    /* Predictive scrolling begins before the next timestamp. Start dimming
       the outgoing line at the same moment so it settles with the scroll. */
    .lyrics-line.scroll-exiting {
      opacity: 0.8;
      color: var(--lyplus-text-secondary);
      transition:
        opacity var(--scroll-duration, 400ms) cubic-bezier(0.41, 0, 0.12, 0.99),
        transform var(--scroll-duration, 400ms)
          cubic-bezier(0.41, 0, 0.12, 0.99) var(--lyrics-line-delay, 0ms),
        filter var(--scroll-duration, 400ms) ease;
    }

    .lyrics-line.persist-highlight {
      filter: none !important;
      opacity: 1;
    }

    .lyrics-line.persist-highlight .lyrics-syllable.finished,
    .lyrics-line.persist-highlight .lyrics-syllable.finished span.char {
      transition: none !important;
    }

    .lyrics-line.singer-right {
      text-align: end;
    }

    .lyrics-line.singer-right .lyrics-line-container,
    .lyrics-line.singer-right .main-vocal-container {
      transform-origin: right;
    }

    .lyrics-line.rtl-text {
      direction: rtl;
      text-align: right !important;
      transform-origin: right;
    }

    .lyrics-line.rtl-text .lyrics-line-container,
    .lyrics-line.rtl-text .main-vocal-container {
      transform-origin: right;
    }

    .lyrics-line.rtl-text .lyrics-romanization-container,
    .lyrics-line.rtl-text .lyrics-translation-container {
      text-align: right;
    }

    /* --- Unsynced (Plain Text) Lyrics Overrides --- */
    .lyrics-container.is-unsynced .lyrics-line {
      opacity: 1 !important;
      color: var(--lyplus-text-primary) !important;
      filter: none !important;
      transform: none !important;
      cursor: default;
    }

    .lyrics-container.is-unsynced .lyrics-line-container {
      transform: none !important;
      background-color: transparent !important;
    }

    .lyrics-container.is-unsynced .lyrics-syllable {
      color: var(--lyplus-text-primary) !important;
      background-color: transparent !important;
      -webkit-background-clip: unset !important;
      background-clip: unset !important;
      -webkit-text-fill-color: unset !important;
      text-fill-color: unset !important;
      text-shadow: none !important;
      filter: none !important;
      opacity: 1 !important;
      transform: none !important;
    }

    @media (hover: hover) and (pointer: fine) {
      .lyrics-line:hover {
        filter: none !important;
        opacity: 1 !important;
      }
      .lyrics-container.is-unsynced .lyrics-line:hover {
        background: transparent !important;
      }
    }

    /* --- Blur Effect for Inactive Lines --- */
    .lyrics-container.blur-inactive-enabled:not(.not-focused)
      .lyrics-line:not(.active):not(.pre-active):not(.lyrics-gap):not(
        .persist-highlight
      ) {
      filter: blur(var(--lyplus-blur-amount));
    }

    /* Viewport Virtualization: Strip expensive filters and animations from
       offscreen lines.  IntersectionObserver toggles this class. */
    .lyrics-line.far-line {
      filter: none !important;
      will-change: auto !important;
      animation: none !important;
    }

    .lyrics-container.blur-inactive-enabled:not(.not-focused)
      .lyrics-line.post-active-line:not(.lyrics-gap):not(.active):not(
        .pre-active
      ):not(.persist-highlight),
    .lyrics-container.blur-inactive-enabled:not(.not-focused)
      .lyrics-line.next-active-line:not(.lyrics-gap):not(.active):not(
        .pre-active
      ):not(.persist-highlight),
    .lyrics-container.blur-inactive-enabled:not(.not-focused)
      .lyrics-line.lyrics-activest:not(.active):not(.lyrics-gap):not(
        .pre-active
      ):not(.persist-highlight) {
      filter: blur(var(--lyplus-blur-amount-near));
    }

    /* Unblur all lines when user is scrolling */
    .lyrics-container.user-scrolling .lyrics-line {
      transition: none !important;
      filter: none !important;
      opacity: 0.8 !important;
    }

    /* Unblur early for pre-active lines */
    .lyrics-container.blur-inactive-enabled .lyrics-line.pre-active {
      filter: blur(0px) !important;
      opacity: 1;
    }

    /* ==========================================================================
       WORD & SYLLABLE STYLES
       ========================================================================== */
    .lyrics-word:not(.allow-break) {
      display: inline-block;
      vertical-align: baseline;
      white-space: nowrap;
    }

    .lyrics-word.allow-break {
      display: inline;
    }

    .lyrics-word.char-rise {
      display: inline-block;
      vertical-align: baseline;
      white-space: nowrap;
    }

    .lyrics-word.char-drag {
      display: inline-block;
      vertical-align: baseline;
      white-space: nowrap;
    }

    .lyrics-word.char-rise.allow-break {
      display: inline;
      white-space: normal;
    }

    .lyrics-word.char-drag.allow-break {
      display: inline;
      white-space: normal;
    }

    .lyrics-syllable-wrap {
      display: inline;
    }

    .lyrics-syllable-wrap.has-transliteration {
      display: inline-flex;
      flex-direction: column;
      align-items: start;
    }

    .lyrics-syllable {
      display: inline-block;
      vertical-align: baseline;
      color: transparent;
      background-color: var(--lyplus-text-secondary);
      white-space: pre-wrap;
      font-variant-ligatures: none;
      font-feature-settings: 'liga' 0;
      background-clip: text;
      -webkit-background-clip: text;
      transition:
        color 0.7s,
        background-color 0.7s,
        transform 0.7s ease;
    }

    /* --- Syllable States --- */
    .lyrics-syllable.finished {
      background-color: var(--lyplus-text-primary);
      /* Unified transition: transform keeps its 1s glow decay, while
         background-color and color fade at 0.7s so everything dims
         together when the line becomes inactive. */
      transition:
        transform 1s ease,
        background-color 0.7s ease,
        color 0.7s ease;
    }

    .lyrics-syllable.finished.has-chars {
      background-color: transparent;
    }

    .lyrics-line.active:not(.lyrics-gap) .lyrics-syllable {
      transition:
        transform 1s ease,
        background-color 0.5s,
        color 0.5s;
    }

    /* --- Wipe Highlight Effect --- */
    .lyrics-line.active:not(.lyrics-gap) .lyrics-syllable.highlight.no-chars,
    .lyrics-line.active:not(.lyrics-gap)
      .lyrics-syllable.pre-highlight.no-chars {
      background-repeat: no-repeat;
      background-image: linear-gradient(
        90deg,
        var(--lyplus-text-primary, #fff) 0%,
        var(--lyplus-text-primary, #fff)
          calc(100% - var(--wipe-gradient-width, 0.75em)),
        #0000 100%
      );
      background-size: 0% 100%;
      background-position: left;
    }

    .lyrics-line.active:not(.lyrics-gap) .lyrics-syllable.highlight.rtl-text,
    .lyrics-line.active:not(.lyrics-gap)
      .lyrics-syllable.pre-highlight.rtl-text {
      direction: rtl;
      background-image: linear-gradient(
        -90deg,
        var(--lyplus-text-primary) 0%,
        var(--lyplus-text-primary)
          calc(100% - var(--wipe-gradient-width, 0.75em)),
        transparent 100%
      );
      background-size: 0% 100%;
      background-position: right 0%;
    }

    /* Background vocals: muted gray wipe instead of white.
       Must match specificity of the main .active .highlight rule (0,3,1). */
    .lyrics-line.active
      .background-vocal-container
      .lyrics-syllable.highlight.no-chars,
    .lyrics-line.active
      .background-vocal-container
      .lyrics-syllable.pre-highlight.no-chars,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.highlight.no-chars,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.pre-highlight.no-chars {
      background-image: linear-gradient(
        90deg,
        color-mix(in srgb, var(--lyplus-text-primary, #fff) 50%, #888888) 0%,
        color-mix(in srgb, var(--lyplus-text-primary, #fff) 50%, #888888)
          calc(100% - var(--wipe-gradient-width, 0.75em)),
        #0000 100%
      );
    }

    .lyrics-line.active
      .background-vocal-container
      .lyrics-syllable.highlight.rtl-text,
    .lyrics-line.active
      .background-vocal-container
      .lyrics-syllable.pre-highlight.rtl-text,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.highlight.rtl-text,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.pre-highlight.rtl-text {
      background-image: linear-gradient(
        -90deg,
        color-mix(in srgb, var(--lyplus-text-primary) 50%, #888888) 0%,
        color-mix(in srgb, var(--lyplus-text-primary) 50%, #888888)
          calc(100% - var(--wipe-gradient-width, 0.75em)),
        transparent 100%
      );
    }

    /* Non-growable words float up with a gentle curve */
    .lyrics-line.active:not(.lyrics-gap)
      .lyrics-word:not(.growable)
      .lyrics-syllable.highlight {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    .lyrics-line.persist-highlight:not(.lyrics-gap)
      .lyrics-word:not(.growable)
      .lyrics-syllable.finished {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    .lyrics-word.growable .lyrics-syllable.cleanup .char {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    .lyrics-word.char-rise .lyrics-syllable.cleanup .char {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    .lyrics-word.char-drag .lyrics-syllable.cleanup .char {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    .lyrics-line.persist-highlight
      .lyrics-word.growable
      .lyrics-syllable.finished
      .char,
    .lyrics-line.persist-highlight
      .lyrics-word.char-rise
      .lyrics-syllable.finished
      .char,
    .lyrics-line.persist-highlight
      .lyrics-word.char-drag
      .lyrics-syllable.finished
      .char {
      transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
    }

    /* Background vocal overrides — placed AFTER main rules so they win
       on equal specificity. */
    .background-vocal-container .lyrics-syllable {
      background-color: color-mix(
        in srgb,
        var(--lyplus-text-secondary) 50%,
        #888888
      );
    }

    .lyrics-line.active:not(.lyrics-gap)
      .background-vocal-container
      .lyrics-syllable.finished,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.finished {
      background-color: color-mix(
        in srgb,
        var(--lyplus-text-primary) 50%,
        #888888
      );
    }

    .background-vocal-container .lyrics-syllable.line-synced {
      color: color-mix(
        in srgb,
        var(--lyplus-text-secondary) 50%,
        #888888
      ) !important;
    }

    .lyrics-line.active:not(.lyrics-gap)
      .background-vocal-container
      .lyrics-syllable.line-synced,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.line-synced {
      color: color-mix(
        in srgb,
        var(--lyplus-text-primary) 50%,
        #888888
      ) !important;
    }

    .lyrics-line.active:not(.lyrics-gap)
      .background-vocal-container
      .lyrics-syllable.line-synced.finished,
    .lyrics-line.pre-active
      .background-vocal-container
      .lyrics-syllable.line-synced.finished {
      color: color-mix(
        in srgb,
        var(--lyplus-text-primary) 50%,
        #888888
      ) !important;
    }

    .lyrics-syllable.pre-highlight {
      animation-name: pre-wipe-universal;
      animation-duration: var(--pre-wipe-duration);
      animation-delay: var(--pre-wipe-delay);
      animation-timing-function: linear;
      animation-fill-mode: forwards;
    }

    .lyrics-syllable.pre-highlight.rtl-text {
      animation-name: pre-wipe-universal-rtl;
    }

    .lyrics-syllable.transliteration {
      font-size: var(--lyplus-font-size-subtext);
      white-space: pre-wrap;
      pointer-events: none;
      user-select: none;
    }

    /* Syllable with chars: make syllable transparent, chars handle color */
    .lyrics-line .lyrics-syllable.has-chars:not(.finished) {
      background-color: transparent;
      color: transparent;
    }

    .lyrics-syllable span.char {
      display: inline-block;
      background-color: var(--lyplus-text-secondary);
      white-space: break-spaces;
      font-variant-ligatures: none;
      font-feature-settings: 'liga' 0;
      background-clip: text;
      -webkit-background-clip: text;
      backface-visibility: hidden;
      transform-origin: 50% 80%;
      transition:
        color 0.7s,
        background-color 0.7s,
        transform 0.7s ease;
    }

    .lyrics-syllable.finished span.char {
      background-color: var(--lyplus-text-primary);
      transition:
        color 0.7s,
        background-color 0.7s,
        transform 0.7s ease;
    }

    .lyrics-word.char-drag span.char {
      transition: color 0.18s;
    }

    /* Active char spans: structural only, wipe animation sets gradient */
    .lyrics-line.active .lyrics-syllable span.char {
      background-clip: text;
      -webkit-background-clip: text;
      background-repeat: no-repeat;
      background-image:
        linear-gradient(
          90deg,
          #ffffff00 0%,
          var(--lyplus-text-primary, #fff) 50%,
          #0000 100%
        ),
        linear-gradient(
          90deg,
          var(--lyplus-text-primary, #fff) 100%,
          #0000 100%
        );
      background-size:
        var(--wipe-gradient-width, 0.75em) 100%,
        0% 100%;
      background-position:
        calc(-1 * var(--wipe-gradient-width, 0.75em)) 0%,
        left;
      transition:
        transform 0.7s ease,
        color 0.18s;
    }

    .lyrics-line.active .lyrics-syllable span.char.highlight {
      background-image: linear-gradient(
        -90deg,
        var(--lyplus-text-primary, #fff) 0%,
        var(--lyplus-text-primary, #fff)
          calc(100% - var(--wipe-gradient-width, 0.75em)),
        #0000 100%
      );
      background-size: 0% 100%;
      background-position: right 0%;
    }

    .lyrics-line.active .lyrics-syllable span.char.pre-wipe-lead {
      animation-name: char-pre-wipe;
      animation-duration: var(--pre-wipe-duration);
      animation-delay: var(--pre-wipe-delay);
      animation-timing-function: linear;
      animation-fill-mode: forwards;
    }

    /* ==========================================================================
       INSTRUMENTAL GAP STYLES
       ========================================================================== */
    .lyrics-gap {
      max-height: 1.6em;
      padding: var(--lyplus-padding-gap);
      overflow: visible;
      opacity: 0;
      box-sizing: content-box;
      background-clip: unset;
      transform-origin: top;
      content-visibility: visible !important;
      contain: none !important;
      transition:
        opacity 160ms ease-out,
        transform var(--scroll-duration, 280ms) var(--lyrics-line-delay, 0ms);
    }

    .lyrics-gap.active {
      opacity: 1;
      transition:
        opacity 160ms ease-out,
        transform var(--scroll-duration, 280ms);
    }

    /* Exiting state: quickly collapse width and height so dots don't distort page, or remove max-height transition */
    .lyrics-gap.gap-exiting {
      opacity: 1;
    }

    .lyrics-gap .main-vocal-container {
      transform: translateY(-25%) scale(1);
      transition: transform 400ms cubic-bezier(0.22, 1, 0.36, 1);
    }

    .lyrics-gap:not(.active):not(.gap-exiting) .main-vocal-container {
      transform: translateY(-25%) scale(0);
    }

    /* Pulse — must come BEFORE .gap-exiting so exiting wins via specificity+order */
    .lyrics-gap.active .main-vocal-container {
      animation: gap-loop var(--gap-pulse-duration, 4000ms) ease-in-out infinite
        alternate;
      animation-delay: var(--gap-loop-delay, 0ms);
    }

    /* Jump animation plays during exit — disable transition so animation wins.
       Placed AFTER .active so it wins when both classes are present briefly. */
    .lyrics-gap.gap-exiting .main-vocal-container {
      animation: gap-ended var(--gap-exit-duration, 360ms)
        cubic-bezier(0.33, 1, 0.68, 1) forwards;
      transition: none !important;
    }

    .lyrics-gap .lyrics-syllable {
      display: inline-block;
      width: var(--lyplus-gap-dot-size);
      height: var(--lyplus-gap-dot-size);
      background-color: var(--lyplus-text-primary);
      border-radius: 50%;
      margin: 0 var(--lyplus-gap-dot-margin);
    }

    /* Line-synced lyrics should fade in instantly/quickly instead of wiping */
    .lyrics-syllable.line-synced {
      background: transparent !important;
      color: var(--lyplus-text-secondary) !important;
    }

    .lyrics-line.active .lyrics-syllable.line-synced {
      animation: fade-in-line 0.2s ease-out forwards !important;
      color: var(--lyplus-text-primary) !important;
    }

    .lyrics-line.active .lyrics-syllable.line-synced span.char {
      background-image: none !important;
      background-color: var(--lyplus-text-primary) !important;
      transition: background-color 120ms ease-out !important;
    }

    @keyframes fade-in-line {
      from {
        opacity: 0.5;
        color: var(--lyplus-text-secondary);
      }
      to {
        opacity: 1;
        color: var(--lyplus-lyrics-palette);
      }
    }

    .lyrics-gap .lyrics-syllable {
      background-color: var(--lyplus-text-secondary);
      background-clip: unset;
    }

    .lyrics-gap.active .lyrics-syllable.finished,
    .lyrics-gap.gap-exiting .lyrics-syllable.finished,
    .lyrics-gap:not(.active):not(.gap-exiting).post-active-line
      .lyrics-syllable,
    .lyrics-gap:not(.active):not(.gap-exiting).lyrics-activest
      .lyrics-syllable {
      background-color: var(--lyplus-text-primary);
      animation: none !important;
      opacity: 1;
    }

    /* ==========================================================================
       METADATA & FOOTER STYLES
       ========================================================================== */
    .lyrics-plus-metadata {
      display: block;
      position: relative;
      box-sizing: border-box;
      font-weight: normal;
      transform: translateY(var(--lyrics-scroll-offset, 0px));
      transition:
        opacity 0.3s ease,
        transform 0.6s cubic-bezier(0.23, 1, 0.32, 1)
          var(--lyrics-line-delay, 0ms),
        filter 0.3s ease;
    }

    .lyrics-plus-empty {
      display: block;
      height: 100vh;
      transform: translateY(var(--lyrics-scroll-offset, 0px));
    }

    .lyrics-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      text-align: left;
      font-size: calc(var(--lyplus-font-size-base) * 0.5);
      color: var(--lyplus-text-secondary);
      padding: 20px 0 50vh 0;
      margin-top: 10px;
      font-weight: 400;
      opacity: 0.8;
      transition:
        opacity 0.3s ease,
        transform 0.5s cubic-bezier(0.41, 0, 0.12, 0.99),
        filter 0.3s ease;
      transform-origin: left;
    }

    .lyrics-footer.lyrics-line {
      font-size: calc(var(--lyplus-font-size-base) * 0.5);
      padding: 20px var(--lyplus-padding-line) 50vh var(--lyplus-padding-line);
      margin-top: 0;
    }

    .lyrics-footer.active {
      opacity: 1;
      color: rgba(255, 255, 255, 0.5); /* Grey instead of primary */
    }

    .lyrics-footer.scroll-animate {
      transition: none !important;
      animation-name: lyrics-scroll;
      animation-duration: var(--scroll-duration, 280ms);
      animation-timing-function: cubic-bezier(0.41, 0, 0.12, 0.99);
      animation-fill-mode: both;
      animation-delay: var(--lyrics-line-delay, 0ms);
    }

    .lyrics-container.blur-inactive-enabled:not(.not-focused)
      .lyrics-footer:not(.active) {
      filter: blur(var(--lyplus-blur-amount));
      opacity: 0.5;
    }

    .lyrics-container.user-scrolling .lyrics-footer {
      transition: none !important;
      filter: none !important;
      opacity: 0.8 !important;
    }

    .lyrics-footer p {
      margin: 5px 0;
    }

    .lyrics-footer a {
      color: var(--lyplus-text-primary); /* Stand out using primary color */
      text-underline-offset: 2px;
      opacity: 0.8;
      transition: opacity 0.2s;
    }

    .lyrics-footer a:hover {
      opacity: 1;
    }

    .footer-content {
      display: flex;
      align-items: flex-start;
      flex-direction: column;
      gap: 8px;
    }

    .footer-controls {
      display: flex;
      align-items: center;
    }

    /* ==========================================================================
       HEADER & CONTROLS
       ========================================================================== */
    .lyrics-header {
      display: flex;
      padding: 10px 0;
      margin-bottom: 10px;
      gap: 10px;
      justify-content: space-between;
      align-items: center;
    }

    .lyrics-header .download-button {
      background: none;
      border: none;
      cursor: pointer;
      color: #aaa;
      padding: 0;
      margin-left: 10px;
      vertical-align: middle;
      display: inline-flex;
      align-items: center;
      font-family: inherit;
    }

    .lyrics-header .download-button:hover {
      color: rgba(255, 255, 255, 0.9);
    }

    .header-controls {
      display: flex;
      gap: 8px;
    }

    .download-controls {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .source-switch-btn {
      position: relative;
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border: 1px solid rgba(255, 255, 255, 0.2);
      min-height: 28px;
      background: transparent;
      border-radius: 6px;
      color: #aaa;
      cursor: pointer;
      font-family: inherit;
      font-size: 11px;
      transition:
        color 0.2s ease,
        border-color 0.2s ease,
        background-color 0.2s ease,
        transform 0.12s ease;
    }

    .source-switch-btn::before {
      content: '';
      position: absolute;
      inset: -6px;
    }

    .source-switch-btn:active:not(:disabled) {
      transform: scale(0.96);
    }

    .source-switch-btn:disabled {
      cursor: default;
      opacity: 0.7;
    }

    .source-switch-svg {
      margin-right: 4px;
    }

    .source-switch-svg.is-loading {
      animation: source-switch-spin 1s linear infinite;
    }

    .control-button {
      background: transparent;
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 4px;
      padding: 2px 8px;
      font-size: 0.8em;
      color: rgba(255, 255, 255, 0.6);
      cursor: pointer;
      transition:
        color 0.2s,
        border-color 0.2s,
        background-color 0.2s;
      font-weight: normal;
    }

    .control-button:hover {
      color: rgba(255, 255, 255, 0.9);
      border-color: rgba(255, 255, 255, 0.5);
    }

    .control-button.active {
      background-color: var(--lyplus-text-primary);
      border-color: var(--lyplus-text-primary);
      color: #000;
    }

    .format-select {
      background: transparent;
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 4px;
      color: rgba(255, 255, 255, 0.6);
      font-size: 0.8em;
      margin-left: 10px;
      padding: 2px 5px;
      cursor: pointer;
      font-weight: normal;
      font-family: inherit;
    }

    .format-select:hover {
      color: rgba(255, 255, 255, 0.9);
      border-color: rgba(255, 255, 255, 0.5);
    }

    .format-select option {
      background: #1a1a1a;
      color: #fff;
    }

    /* ==========================================================================
       TRANSLATION & ROMANIZATION
       ========================================================================== */
    .lyrics-translation-container,
    .lyrics-romanization-container {
      padding-top: 0.2em;
      opacity: 0.8;
      font-size: var(--lyplus-font-size-subtext);
      overflow-wrap: break-word;
      pointer-events: none;
      user-select: none;
      transition:
        opacity 0.3s ease,
        color 0.3s;
      font-weight: normal;
    }

    .lyrics-romanization-container {
      direction: ltr !important;
    }

    .lyrics-romanization-container.rtl-text {
      direction: rtl !important;
      text-align: right;
    }

    .lyrics-romanization-container .lyrics-syllable {
      white-space: pre-wrap;
    }

    .lyrics-translation-container {
      opacity: 0.5;
    }

    .main-line-wrapper.small {
      font-size: 0.5em;
      opacity: 0.8;
      display: block;
      margin-bottom: 0px;
    }

    .translation-line {
      font-size: 1em;
      font-weight: bold;
      display: block;
      margin-top: 0px;
      line-height: 1.1;
    }

    .romanized-line {
      font-size: 0.5em;
      color: rgba(255, 255, 255, 0.5);
      display: block;
      margin-top: 2px;
      font-weight: normal;
    }

    /* ==========================================================================
       SKELETON LOADING
       ========================================================================== */
    @keyframes skeleton-loading {
      0% {
        background-color: rgba(255, 255, 255, 0.1);
      }
      100% {
        background-color: rgba(255, 255, 255, 0.2);
      }
    }

    .skeleton-line {
      height: 2.5em;
      margin: 20px 0;
      border-radius: 8px;
      animation: skeleton-loading 1s linear infinite alternate;
      opacity: 0.7;
      width: 60%;
    }

    .skeleton-line:nth-child(even) {
      width: 80%;
    }
    .skeleton-line:nth-child(3n) {
      width: 50%;
    }
    .skeleton-line:nth-child(5n) {
      width: 70%;
    }

    .no-lyrics {
      color: rgba(255, 255, 255, 0.5);
      font-size: 1.2em;
      text-align: center;
      padding: 2em;
      font-weight: normal;
    }

    /* ==========================================================================
       KEYFRAME ANIMATIONS
       ========================================================================== */

    @keyframes source-switch-spin {
      to {
        transform: rotate(360deg);
      }
    }

    /* Wipe animation for syllables */
    @keyframes wipe {
      from {
        background-size: 0% 100%;
        background-position: left;
      }
      to {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: left;
      }
    }

    @keyframes wipe-from-pre {
      from {
        background-size: var(--wipe-gradient-width, 0.75em) 100%;
        background-position: left;
      }
      to {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: left;
      }
    }

    @keyframes start-wipe {
      0% {
        background-size: 0% 100%;
        background-position: left;
      }
      100% {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: left;
      }
    }

    @keyframes wipe-rtl {
      from {
        background-size: 0% 100%;
        background-position: right 0%;
      }
      to {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: right 0%;
      }
    }

    @keyframes wipe-from-pre-rtl {
      from {
        background-size: var(--wipe-gradient-width, 0.75em) 100%;
        background-position: right 0%;
      }
      to {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: right 0%;
      }
    }

    @keyframes start-wipe-rtl {
      0% {
        background-size: 0% 100%;
        background-position: right 0%;
      }
      100% {
        background-size: calc(100% + var(--wipe-gradient-width, 0.75em)) 100%;
        background-position: right 0%;
      }
    }

    @keyframes pre-wipe-universal {
      from {
        background-size: 0% 100%;
        background-position: left;
      }
      to {
        background-size: var(--wipe-gradient-width, 0.75em) 100%;
        background-position: left;
      }
    }

    @keyframes pre-wipe-universal-rtl {
      from {
        background-size: 0% 100%;
        background-position: right 0%;
      }
      to {
        background-size: var(--wipe-gradient-width, 0.75em) 100%;
        background-position: right 0%;
      }
    }

    /* Character-rendered words use a separate moving gradient in front of
       their solid fill. This makes the individual glyph wipes read as one
       continuous word-level wipe. */
    @keyframes char-pre-wipe {
      from {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          0% 100%;
        background-position:
          calc(-1 * var(--wipe-gradient-width, 0.75em)) 0%,
          left;
      }
      to {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          0% 100%;
        background-position:
          calc(-1 * var(--wipe-gradient-half, 0.375em)) 0%,
          left;
      }
    }

    @keyframes char-start-wipe {
      from {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          0% 100%;
        background-position:
          calc(-1 * var(--wipe-gradient-width, 0.75em)) 0%,
          left;
      }
      to {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          100% 100%;
        background-position:
          calc(100% + var(--wipe-gradient-half, 0.375em)) 0%,
          left;
      }
    }

    @keyframes char-wipe {
      from {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          0% 100%;
        background-position:
          calc(-1 * var(--wipe-gradient-half, 0.375em)) 0%,
          left;
      }
      to {
        background-size:
          var(--wipe-gradient-width, 0.75em) 100%,
          100% 100%;
        background-position:
          calc(100% + var(--wipe-gradient-half, 0.375em)) 0%,
          left;
      }
    }

    /* Gap dot animations */
    @keyframes gap-loop {
      from {
        transform: translateY(-25%) scale(1.12);
      }
      to {
        transform: translateY(-25%) scale(var(--gap-exit-scale, 0.85));
      }
    }

    @keyframes gap-ended {
      0% {
        transform: translateY(-25%) scale(var(--gap-exit-scale, 0.85));
      }
      35% {
        transform: translateY(-25%) scale(1.2);
      }
      100% {
        transform: translateY(-25%) scale(0);
      }
    }

    @keyframes fade-gap {
      from {
        background-color: var(--lyplus-text-secondary);
      }
      to {
        background-color: var(--lyplus-text-primary);
      }
    }

    /* Scroll animation — class is removed and re-added (with a forced
       reflow in between) to reliably restart the animation each time */
    @keyframes lyrics-scroll {
      from {
        transform: translate3d(0, var(--scroll-delta), 0);
      }
      to {
        transform: translate3d(0, 0, 0);
      }
    }

    /* Character grow animation — translate3d+scale3d for smooth transform,
       drop-shadow for glow */
    @keyframes grow-dynamic {
      0% {
        transform: translate3d(0, 0, 0) scale3d(1, 1, 1);
        filter: drop-shadow(
          0 0 0
            color-mix(in srgb, var(--lyplus-lyrics-palette), transparent 100%)
        );
      }
      25%,
      30% {
        transform: translate3d(
            var(--char-offset-x, 0px),
            var(--translate-y-peak, -2px),
            0
          )
          scale3d(var(--matrix-scale, 1.1), var(--matrix-scale, 1.1), 1);
        filter: drop-shadow(
          0 0 0.1em
            color-mix(
              in srgb,
              var(--lyplus-lyrics-palette),
              transparent calc((1 - var(--shadow-intensity, 1)) * 100%)
            )
        );
      }
      75%,
      100% {
        transform: translate3d(0, var(--char-rise-y, -1.12px), 0)
          scale3d(1, 1, 1);
        filter: drop-shadow(
          0 0 0
            color-mix(in srgb, var(--lyplus-lyrics-palette), transparent 100%)
        );
      }
    }

    @keyframes rise-char {
      0% {
        transform: translate3d(0, 0, 0);
      }
      65%,
      100% {
        transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
      }
    }

    @keyframes drag-char {
      0% {
        transform: translate3d(0, 0, 0);
      }
      100% {
        transform: translate3d(0, var(--char-rise-y, -1.12px), 0);
      }
    }

    @keyframes grow-static {
      0%,
      100% {
        transform: scale3d(1.01, 1.01, 1.1) translateY(-0.05%);
        text-shadow: 0 0 0
          color-mix(in srgb, var(--lyplus-lyrics-palette), transparent 100%);
      }
      30%,
      40% {
        transform: scale3d(1.1, 1.1, 1.1) translateY(-0.05%);
        text-shadow: 0 0 0.3em
          color-mix(in srgb, var(--lyplus-lyrics-palette), transparent 50%);
      }
    }

    /* Fade in animation */
    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 0.7;
        transform: translateY(0);
      }
    }

    /* Legacy support */
    .opposite-turn {
      text-align: right;
    }

    .singer-right {
      text-align: right;
      justify-content: flex-end;
    }

    .singer-left {
      text-align: left;
      justify-content: flex-start;
    }

    /* Legacy progress-text for backward compatibility */
    .progress-text {
      position: relative;
      display: inline-block;
      background: linear-gradient(
        to right,
        var(--lyplus-text-primary) 0%,
        var(--lyplus-text-primary) var(--line-progress, 0%),
        var(--lyplus-text-secondary) var(--line-progress, 0%),
        var(--lyplus-text-secondary) 100%
      );
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
      color: var(--lyplus-text-secondary);
      transform: translate3d(0, 0, 0);
      will-change: background-size;
    }

    .progress-text::before {
      display: none;
    }

    .active-line {
      font-weight: bold;
    }

    .background-text {
      display: block;
      color: var(--lyplus-text-secondary);
      font-size: 0.8em;
      font-style: normal;
      margin: 0;
      flex-shrink: 0;
      line-height: 1.1;
    }

    .background-text.before {
      order: -1;
    }

    .background-text.after {
      order: 1;
    }

    .instrumental-line {
      display: inline-flex;
      align-items: baseline;
      gap: 8px;
      color: var(--lyplus-text-secondary);
      font-size: 0.9em;
      padding: 4px 10px;
      animation: fadeInUp 220ms ease;
      font-weight: normal;
    }

    .instrumental-duration {
      color: var(--lyplus-text-secondary);
      font-size: 0.8em;
    }
  `,F([B({type:String})],d.prototype,"query",void 0),F([B({type:String})],d.prototype,"musicId",void 0),F([B({type:String})],d.prototype,"isrc",void 0),F([B({type:String})],d.prototype,"ttml",void 0),F([B({type:String,attribute:"song-title"})],d.prototype,"songTitle",void 0),F([J()],d.prototype,"downloadFormat",void 0),F([B({type:String,attribute:"song-artist"})],d.prototype,"songArtist",void 0),F([B({type:String,attribute:"song-album"})],d.prototype,"songAlbum",void 0),F([B({type:String,attribute:"songwriters"})],d.prototype,"songwriters",void 0),F([B({type:Number,attribute:"song-duration"})],d.prototype,"songDurationMs",void 0),F([B({type:String,attribute:"highlight-color"})],d.prototype,"highlightColor",void 0),F([B({type:String,attribute:"font-family"})],d.prototype,"fontFamily",void 0),F([B({type:Boolean})],d.prototype,"autoScroll",void 0),F([B({type:Boolean})],d.prototype,"interpolate",void 0),F([J()],d.prototype,"showRomanization",void 0),F([J()],d.prototype,"showTranslation",void 0),F([B({type:Number})],d.prototype,"duration",void 0),F([B({type:Number,attribute:"currenttime",hasChanged:()=>!1})],d.prototype,"currentTime",null),F([J()],d.prototype,"isLoading",void 0),F([J()],d.prototype,"lyrics",void 0),F([J()],d.prototype,"lyricsSource",void 0),F([J()],d.prototype,"availableSources",void 0),F([J()],d.prototype,"currentSourceIndex",void 0),F([si(".lyrics-container")],d.prototype,"lyricsContainer",void 0),window.customElements.define("am-lyrics",d),Ce}var We=zi();const Di=Ri(We),Ki=Fi({__proto__:null,default:Di},[We]);export{Ki as a};
