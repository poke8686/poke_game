#!/usr/bin/env node
/**
 * Generates spot-the-difference image pairs as SVG cartoon scenes.
 * Each scene has distinct drawn objects; b.svg changes specific objects
 * (color swap, size change, missing element) — clean cartoon-style diffs.
 *
 * Run: node scripts/generate-spotdiff-svg.js
 */
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const ROOT     = path.join(__dirname, '..');
const OUT_DIR  = path.join(ROOT, 'public', 'spotdiff');
const META_PATH = path.join(ROOT, 'data', 'spotdiff', 'stages.json');

const W = 1200, H = 800;

// ─── SVG helpers ─────────────────────────────────────────────────────────────
const rect   = (x,y,w,h,fill,rx=0,opts='') => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" rx="${rx}" ${opts}/>`;
const circle = (cx,cy,r,fill,opts='')       => `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${fill}" ${opts}/>`;
const ellipse= (cx,cy,rx,ry,fill,opts='')   => `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fill}" ${opts}/>`;
const poly   = (pts,fill,opts='')            => `<polygon points="${pts}" fill="${fill}" ${opts}/>`;
const txt    = (x,y,s,fill='#333',size=32)  => `<text x="${x}" y="${y}" font-size="${size}" fill="${fill}" text-anchor="middle" font-family="Arial,sans-serif">${s}</text>`;
const ln     = (x1,y1,x2,y2,stroke,w=4)    => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${w}" stroke-linecap="round"/>`;
const svgDoc = body => `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${body}</svg>`;

// ─── Scenes ──────────────────────────────────────────────────────────────────

function scenepark({ benchColor='#8B4513', balloonColors=['#E74C3C','#F39C12','#9B59B6'],
  flowerCount=4, birdVisible=true, cloudCount=3, treeColor='#27AE60' }={}) {
  let s = '';
  s += rect(0,0,W,H/2,'#87CEEB'); s += rect(0,H/2,W,H/2,'#7EC850');
  s += circle(100,90,55,'#F9CA24');
  const clouds=[{x:200,y:80},{x:500,y:60},{x:850,y:100},{x:1050,y:75}];
  for(let i=0;i<cloudCount;i++){const c=clouds[i];if(!c)break;s+=ellipse(c.x,c.y,70,35,'white');s+=ellipse(c.x-45,c.y+10,45,30,'white');s+=ellipse(c.x+45,c.y+10,45,30,'white');}
  [{x:150},{x:900},{x:1050}].forEach(t=>{s+=rect(t.x-12,380,24,160,'#7D5A1E');s+=circle(t.x,350,75,treeColor);s+=circle(t.x-40,370,55,treeColor);s+=circle(t.x+40,370,55,treeColor);});
  s+=rect(450,490,300,18,benchColor,4);s+=rect(460,508,16,80,benchColor,2);s+=rect(720,508,16,80,benchColor,2);s+=rect(452,475,296,20,benchColor,2);
  s+=ellipse(950,620,120,50,'#5DADE2');s+=ellipse(950,620,100,40,'#85C1E9');
  const fp=[{x:350,y:570},{x:380,y:555},{x:620,y:580},{x:650,y:560},{x:700,y:575},{x:200,y:600}];
  const fc=['#E74C3C','#F39C12','#9B59B6','#E91E63','#FF5722'];
  for(let i=0;i<flowerCount;i++){const p=fp[i];if(!p)break;s+=rect(p.x,p.y,4,30,'#27AE60');s+=circle(p.x+2,p.y,10,fc[i%fc.length]);s+=circle(p.x-8,p.y-4,7,fc[i%fc.length]);s+=circle(p.x+12,p.y-4,7,fc[i%fc.length]);}
  const bp=[{x:540,y:350},{x:590,y:320},{x:640,y:345}];
  balloonColors.forEach((col,i)=>{if(!bp[i])return;s+=circle(bp[i].x,bp[i].y,28,col);s+=ln(bp[i].x,bp[i].y+28,bp[i].x-5,bp[i].y+70,'#555',2);});
  if(birdVisible){s+=`<path d="M700,200 Q720,185 740,200" stroke="#333" stroke-width="3" fill="none"/>`;s+=`<path d="M740,200 Q760,185 780,200" stroke="#333" stroke-width="3" fill="none"/>`;}
  return s;
}

function scenekitchen({ curtainColor='#E74C3C', appleCount=3, mugColor='#3498DB', potVisible=true }={}) {
  let s='';
  s+=rect(0,0,W,H*0.65,'#FFF9E7');s+=rect(0,H*0.65,W,H*0.35,'#D4B483');
  s+=rect(80,60,240,200,'#AED6F1',8);s+=ln(230,60,230,260,'#888',3);s+=ln(80,160,320,160,'#888',3);
  s+=rect(80,60,240,200,'none',0,`stroke="#888" stroke-width="4" fill="none"`);
  s+=poly(`80,60 150,60 120,260 80,260`,curtainColor);s+=poly(`320,60 250,60 280,260 320,260`,curtainColor);
  s+=rect(400,100,420,18,'#A0522D',3);
  for(let i=0;i<3;i++){s+=circle(460+i*130,90,38,'#ECF0F1',`stroke="#BDC3C7" stroke-width="3"`);s+=circle(460+i*130,90,28,'#E8DAEF');}
  s+=rect(0,480,W,60,'#F0E6D3');s+=rect(0,540,W,H-540,'#D4B483');
  s+=rect(500,340,280,160,'#BDC3C7',6);[{x:565,y:390},{x:715,y:390},{x:565,y:460},{x:715,y:460}].forEach(b=>{s+=circle(b.x,b.y,35,'#555');});
  if(potVisible){s+=ellipse(640,330,65,22,'#7F8C8D');s+=rect(575,275,130,60,'#95A5A6',4);s+=rect(505,270,20,15,'#7F8C8D',2);s+=rect(755,270,20,15,'#7F8C8D',2);}
  s+=ellipse(250,488,90,28,'#D4AC0D');
  for(let i=0;i<appleCount;i++){s+=circle(185+i*65,465,30,['#E74C3C','#27AE60','#FF8C00'][i%3]);s+=rect(183+i*65,435,4,15,'#27AE60',2);}
  s+=rect(920,440,70,80,mugColor,6);s+=`<path d="M990,455 Q1030,455 1030,490 Q1030,520 990,520" stroke="${mugColor}" stroke-width="8" fill="none"/>`;
  s+=circle(850,160,60,'#ECF0F1',`stroke="#BDC3C7" stroke-width="4"`);s+=ln(850,160,850,115,'#333',4);s+=ln(850,160,895,160,'#333',3);s+=circle(850,160,6,'#333');
  return s;
}

function scenebeach({ umbrellaColor='#E74C3C', castleTowers=3, boatColor='#F39C12', seagullCount=2 }={}) {
  let s='';
  s+=rect(0,0,W,350,'#87CEEB');s+=rect(0,300,W,150,'#3498DB');s+=rect(0,420,W,H-420,'#F4D03F');
  s+=circle(1100,80,65,'#F9CA24');
  s+=`<path d="M0,360 Q150,340 300,360 Q450,380 600,360 Q750,340 900,360 Q1050,380 1200,360 L1200,450 L0,450 Z" fill="#5DADE2" opacity="0.7"/>`;
  s+=rect(380,350,10,160,'#8B6914',4);s+=poly(`230,350 535,350 385,200`,umbrellaColor);s+=poly(`300,350 455,350 380,240`,`${umbrellaColor}cc`);
  s+=rect(200,500,220,80,'#9B59B6',6);
  const tw=[{x:710},{x:760},{x:810}];
  for(let i=0;i<castleTowers;i++){const t=tw[i];if(!t)break;s+=rect(t.x,465,40,65,'#E8B830',4);s+=poly(`${t.x},465 ${t.x+20},435 ${t.x+40},465`,'#D4AC0D');}
  s+=rect(680,510,140,100,'#F0C040',4);
  s+=poly(`850,320 1050,320 1020,370 880,370`,boatColor);s+=rect(930,270,8,55,'#8B6914',2);s+=poly(`938,270 1000,300 938,330`,'white');
  for(let i=0;i<seagullCount;i++){const p=[{x:200,y:150},{x:400,y:120}][i];if(!p)break;s+=`<path d="M${p.x-20},${p.y} Q${p.x},${p.y-15} ${p.x+20},${p.y}" stroke="#555" stroke-width="3" fill="none"/>`;}
  s+=`<g transform="translate(950,560)"><polygon points="0,-25 6,-8 22,-8 10,4 15,22 0,12 -15,22 -10,4 -22,-8 -6,-8" fill="#FF6B35"/></g>`;
  return s;
}

function sceneforest({ mushroomColor='#E74C3C', deerVisible=true, butterflyVisible=true, treeTops=['#2ECC71','#27AE60','#1E8449','#239B56'] }={}) {
  let s='';
  s+=rect(0,0,W,H,'#A8D5A2');s+=rect(0,0,W,250,'#B0E0E6');s+=rect(0,580,W,H-580,'#5D4037');s+=rect(0,560,W,50,'#7EC850');
  [[100,80],[300,100],[450,60],[700,90],[850,70],[1000,85],[1150,75]].forEach(([x,sz])=>{
    s+=rect(x-10,280,20,280,'#5D3A1A');const idx=Math.floor(x/200)%treeTops.length;
    s+=ellipse(x,sz+160,80,90,treeTops[idx]);s+=ellipse(x-40,sz+200,60,70,treeTops[(idx+1)%4]);s+=ellipse(x+40,sz+200,60,70,treeTops[(idx+2)%4]);
  });
  [[350,530],[400,510],[1000,540],[1050,520]].forEach(([x,y],i)=>{
    s+=rect(x-6,y,12,40,'#D7CCC8',2);s+=ellipse(x,y,30,18,i%2===0?mushroomColor:'#F39C12');s+=ellipse(x,y,20,12,'white',`opacity="0.5"`);
    [[-10,-5],[5,-8],[0,0]].forEach(([dx,dy])=>s+=circle(x+dx,y+dy,4,'white'));
  });
  s+=ellipse(800,555,80,45,'#1E8449');s+=ellipse(760,550,65,40,'#27AE60');s+=ellipse(840,548,65,40,'#239B56');
  [790,810,830,770,800].forEach((x)=>s+=circle(x,540,8,'#9B59B6'));
  if(deerVisible){
    s+=ellipse(200,520,45,30,'#D2691E');s+=ellipse(200,490,25,22,'#D2691E');s+=circle(200,478,15,'#C4A265');
    [175,185,210,220].forEach(x=>s+=rect(x,545,8,50,'#A0522D',2));
    s+=ln(194,465,178,440,'#8B4513',3);s+=ln(178,440,165,430,'#8B4513',2);s+=ln(178,440,175,425,'#8B4513',2);
    s+=ln(206,465,222,440,'#8B4513',3);s+=ln(222,440,235,430,'#8B4513',2);s+=circle(200,478,3,'#333');
  }
  if(butterflyVisible){
    s+=`<g transform="translate(650,350)"><ellipse cx="-15" cy="0" rx="25" ry="15" fill="#E91E63" opacity="0.85"/><ellipse cx="15" cy="0" rx="25" ry="15" fill="#9C27B0" opacity="0.85"/><line x1="0" y1="-20" x2="0" y2="20" stroke="#333" stroke-width="2"/></g>`;
  }
  return s;
}

function sceneroom({ bedColor='#5B9BD5', lampColor='#F9CA24', plantPotColor='#E74C3C', catVisible=true }={}) {
  let s='';
  s+=rect(0,0,W,H,'#FFF8F0');s+=rect(0,560,W,H-560,'#D4A574');
  s+=rect(100,60,260,300,'#AED6F1',8);s+=rect(100,60,260,300,'none',0,`stroke="#999" stroke-width="5" fill="none"`);s+=ln(230,60,230,360,'#999',4);s+=ln(100,210,360,210,'#999',4);
  s+=poly(`100,60 170,60 145,360 100,360`,'#E8DAEF');s+=poly(`360,60 290,60 315,360 360,360`,'#E8DAEF');
  s+=rect(500,80,200,160,'#8B4513',4,`stroke="#8B4513" stroke-width="8"`);s+=rect(514,94,172,132,'#AED6F1',2);s+=ellipse(600,160,50,35,'#E8D5B7');
  s+=rect(750,100,150,120,'#8B4513',4,`stroke="#8B4513" stroke-width="6"`);s+=rect(762,112,126,96,'#FFEAA7',2);
  s+=circle(1050,140,65,'#FFFDE7',`stroke="#999" stroke-width="4"`);s+=ln(1050,140,1050,100,'#333',4);s+=ln(1050,140,1090,140,'#555',3);s+=circle(1050,140,6,'#E74C3C');
  s+=rect(150,430,450,150,bedColor,8);s+=rect(150,400,450,55,'#ECF0F1',6,`stroke="${bedColor}" stroke-width="3"`);
  s+=rect(165,410,120,100,'#F0F0F0',4);s+=rect(300,410,120,100,'#F5F5F5',4);
  s+=rect(660,360,16,120,'#A0522D',3);s+=rect(640,470,56,16,'#8B4513',4);s+=poly(`620,360 700,360 685,280 635,280`,lampColor);
  s+=ellipse(380,590,220,55,'#C0392B',`opacity="0.7"`);
  s+=rect(958,480,36,90,plantPotColor,4);s+=ellipse(976,480,40,55,'#27AE60');s+=ellipse(950,470,30,42,'#2ECC71');
  if(catVisible){
    s+=ellipse(870,540,50,35,'#F5CBA7');s+=ellipse(870,505,30,25,'#F5CBA7');
    s+=poly(`850,495 842,470 862,483`,'#F5CBA7');s+=poly(`890,495 898,470 878,483`,'#F5CBA7');
    s+=circle(862,504,5,'#333');s+=circle(878,504,5,'#333');
    s+=`<path d="M855,515 Q870,522 885,515" stroke="#555" stroke-width="2" fill="none"/>`;
  }
  return s;
}

function scenestreet({ buildingColors=['#E74C3C','#3498DB','#F39C12','#9B59B6'], carColor='#27AE60', treeCount=3 }={}) {
  let s='';
  s+=rect(0,0,W,H,'#87CEEB');s+=rect(0,520,W,H-520,'#555');s+=rect(0,480,W,50,'#DDD');
  buildingColors.forEach((col,i)=>{
    const bx=i*250+50,by=120+i*20,bh=H-by-200;
    s+=rect(bx,by,220,bh,col);
    for(let wy=by+30;wy<by+bh-40;wy+=70)for(let wx=bx+25;wx<bx+195;wx+=65){s+=rect(wx,wy,42,45,'#AED6F1',3);s+=ln(wx+21,wy,wx+21,wy+45,'#7FB3D3',1);s+=ln(wx,wy+22,wx+42,wy+22,'#7FB3D3',1);}
    s+=rect(bx+90,by+bh-65,40,65,`${col}88`,3);
  });
  for(let i=0;i<3;i++){const lx=180+i*400;s+=rect(lx,300,12,220,'#555',2);s+=ellipse(lx+6,296,22,12,'#F1C40F');}
  s+=rect(500,490,220,50,carColor,8);s+=rect(530,465,160,32,carColor,8);s+=circle(540,545,28,'#333');s+=circle(540,545,18,'#888');s+=circle(690,545,28,'#333');s+=circle(690,545,18,'#888');
  s+=rect(300,170,160,40,'#E74C3C',6);s+=txt(380,197,'SHOP','white',18);
  for(let i=0;i<treeCount;i++){const tx=80+i*500;s+=rect(tx-8,370,16,115,'#8B6914',3);s+=circle(tx,350,55,'#27AE60');}
  return s;
}

function scenespace({ planetColor='#E74C3C', rocketColor='#BDC3C7', moonColor='#ECF0F1', ringColor='#F39C12' }={}) {
  let s='';
  s+=rect(0,0,W,H,'#0D0D2B');
  for(let i=0;i<35;i++){s+=circle(((i*137+53)%W),((i*97+31)%(H*0.75)),i%5===0?3:1.5,'white',`opacity="${0.5+0.5*(i%3)/2}"`);}
  s+=ellipse(200,200,150,100,'#6C3483',`opacity="0.3"`);
  s+=circle(900,350,180,planetColor);
  s+=ellipse(900,350,250,60,ringColor,`opacity="0.6"`);s+=ellipse(900,350,240,50,'#0D0D2B',`opacity="0.7"`);s+=ellipse(900,350,250,60,'none',`stroke="${ringColor}" stroke-width="4" fill="none" opacity="0.8"`);
  [[-60,-40],[40,20],[-20,60],[80,-30]].forEach(([dx,dy])=>{s+=circle(900+dx,350+dy,18,'#C0392B',`opacity="0.5"`);});
  s+=circle(200,500,100,moonColor);[[-30,-20],[25,15],[-10,35],[40,-25]].forEach(([dx,dy])=>s+=circle(200+dx,500+dy,14,'#D5D8DC'));
  const rx=550,ry=300;
  s+=`<path d="M${rx},${ry-120} L${rx-35},${ry+60} L${rx+35},${ry+60} Z" fill="${rocketColor}"/>`;
  s+=ellipse(rx,ry-110,35,50,'#E74C3C');s+=rect(rx-12,ry-30,24,60,'#AED6F1',4,`opacity="0.8"`);
  s+=poly(`${rx-35},${ry+60} ${rx-60},${ry+90} ${rx-20},${ry+60}`,'#E74C3C');s+=poly(`${rx+35},${ry+60} ${rx+60},${ry+90} ${rx+20},${ry+60}`,'#E74C3C');
  s+=ellipse(rx,ry+70,18,30,'#F39C12',`opacity="0.9"`);
  s+=circle(300,400,45,'#2ECC71');s+=circle(280,388,8,'white');s+=circle(320,388,8,'white');s+=circle(282,389,4,'#111');s+=circle(322,389,4,'#111');
  s+=ln(300,445,300,510,'#2ECC71',8);s+=ln(300,465,260,490,'#2ECC71',6);s+=ln(300,465,340,490,'#2ECC71',6);
  s+=ellipse(700,180,80,25,'#BDC3C7');s+=ellipse(700,168,45,22,'#95A5A6');
  for(let i=0;i<5;i++)s+=circle(640+i*30,183,5,'#F9CA24',`opacity="0.8"`);
  return s;
}

function sceneaquarium({ fish1Color='#E74C3C', coralColor='#FF6B35', bubbleCount=8, chestOpen=false }={}) {
  let s='';
  s+=rect(0,0,W,H,'#D6EAF8');s+=rect(50,50,1100,700,'#1A3A4A',8);s+=rect(60,60,1080,680,'#1B4F72',0);
  s+=rect(60,640,1080,100,'#D4AC0D');
  [[150,640],[400,635],[700,638],[950,642],[1050,636]].forEach(([x])=>{for(let j=0;j<4;j++){s+=ellipse(x+(j%2===0?-15:15),640-j*45,12,30,'#27AE60',`opacity="${0.7+j*0.07}"`);}});
  [[250,640],[600,638],[850,642]].forEach(([x,y],i)=>{const col=[coralColor,'#FF8FAB','#FF6B35'][i];s+=poly(`${x},${y} ${x-30},${y-80} ${x},${y-60} ${x+30},${y-80}`,col);s+=circle(x-30,y-80,10,col);s+=circle(x+30,y-80,10,col);});
  const cx=1000,cy=620;s+=rect(cx,cy-50,90,50,'#8B6914',4);s+=rect(cx,cy-55,90,12,'#A0522D',3);s+=circle(cx+45,cy-50,8,'#FFD700');
  if(chestOpen){s+=rect(cx-5,cy-80,100,35,'#8B6914',3,`transform="rotate(-30,${cx+45},${cy-52})"`);for(let i=0;i<5;i++)s+=circle(cx+20+i*12,cy-60,4,'#FFD700');}
  for(let i=0;i<bubbleCount;i++){const bx=100+i*120,by=200+((i*73)%300);s+=circle(bx,by,8,'none',`stroke="white" stroke-width="2" opacity="0.6"`);}
  function fish(fx,fy,col,sc=1){s+=`<g transform="translate(${fx},${fy}) scale(${sc},${sc})"><ellipse cx="0" cy="0" rx="55" ry="32" fill="${col}"/><polygon points="-55,-30 -90,0 -55,30" fill="${col}" opacity="0.8"/><circle cx="35" cy="-8" r="8" fill="white"/><circle cx="36" cy="-9" r="5" fill="#333"/></g>`;}
  fish(300,300,fish1Color);fish(500,450,'#F39C12',0.8);fish(750,280,fish1Color,0.7);fish(900,400,'#9B59B6',0.9);
  s+=ellipse(200,500,80,100,'#95A5A6');s+=ellipse(200,500,65,85,'#BDC3C7');s+=circle(215,490,10,'white');s+=circle(217,491,6,'#333');
  return s;
}

function scenemarket({ awningColors=['#E74C3C','#3498DB','#F39C12'], appleCount=5, umbrellaColor='#9B59B6', signText='마켓' }={}) {
  let s='';
  s+=rect(0,0,W,H,'#FFF9E7');s+=rect(0,520,W,H-520,'#D4B483');
  [[60,'#FFFDE7'],[430,'#FFF8F0'],[800,'#FFFDE7']].forEach(([x,bg],si)=>{
    s+=rect(x,200,320,340,bg,4,`stroke="#DDD" stroke-width="2"`);
    s+=poly(`${x},200 ${x+320},200 ${x+300},150 ${x+20},150`,awningColors[si]);
    for(let i=0;i<6;i++){const sx=x+20+i*48;s+=poly(`${sx},200 ${sx+24},200 ${sx+20},150 ${sx-4},150`,'white',`opacity="0.25"`);}
    s+=rect(x+10,390,300,25,'#C4956A',3);
  });
  s+=rect(200,80,200,55,signText==='마켓'?'#E74C3C':'#27AE60',6);s+=txt(300,116,signText,'white',24);
  for(let i=0;i<appleCount;i++){s+=circle(90+i*45,370,22,'#E74C3C');s+=rect(99+i*45,345,4,10,'#27AE60',1);}
  for(let i=0;i<3;i++){s+=`<path d="M${480+i*30},350 Q${510+i*30},330 ${530+i*30},355" stroke="#F9CA24" stroke-width="18" fill="none" stroke-linecap="round"/>`;}
  for(let i=0;i<4;i++){s+=circle(490+i*40,385,18,'#E74C3C');s+=rect(488+i*40,368,4,8,'#27AE60',1);}
  [850,920,990].forEach(x=>{s+=`<path d="M${x-30},400 Q${x},420 ${x+30},400 L${x+25},380 Q${x},370 ${x-25},380 Z" fill="#D4AC0D"/>`;[x-15,x,x+15].forEach((bx,bi)=>s+=circle(bx,378,10,['#9B59B6','#E74C3C','#F39C12'][bi]));});
  s+=rect(1080,300,8,250,'#8B6914',3);s+=`<path d="M1000,300 Q1084,240 1170,300" fill="${umbrellaColor}"/>`;
  return s;
}

function scenepicnic({ blanketColor='#E74C3C', sandwichCount=3, juiceColor='#F39C12', antCount=4, kiteVisible=true }={}) {
  let s='';
  s+=rect(0,0,W,H,'#87CEEB');s+=rect(0,450,W,H-450,'#7EC850');
  s+=circle(1080,90,60,'#F9CA24');
  [[200,100],[600,80],[950,120]].forEach(([x,y])=>{s+=ellipse(x,y,80,38,'white');s+=ellipse(x-50,y+12,55,32,'white');s+=ellipse(x+50,y+12,55,32,'white');});
  s+=rect(188,290,20,200,'#8B6914',3);s+=circle(200,270,85,'#27AE60');s+=circle(155,295,65,'#2ECC71');s+=circle(245,295,65,'#1E8449');
  if(kiteVisible){
    s+=poly(`700,150 740,200 700,260 660,200`,'#E74C3C');s+=ln(700,200,700,260,'#FFD700',2);s+=ln(660,200,740,200,'#FFD700',2);
    s+=`<path d="M700,260 Q720,300 710,340" stroke="#888" stroke-width="2" fill="none"/>`;
  }
  s+=rect(350,480,400,200,blanketColor,6);
  s+=`<path d="M750,510 Q800,535 850,510 L845,480 Q800,468 755,480 Z" fill="#D4AC0D"/>`;
  for(let i=0;i<sandwichCount;i++){s+=poly(`${400+i*80},560 ${440+i*80},555 ${445+i*80},580 ${395+i*80},582`,'#F4D03F',`stroke="#C9A227" stroke-width="2"`);s+=rect(400+i*80,556,40,6,'#E74C3C',0,`opacity="0.7"`);}
  s+=rect(600,545,30,55,juiceColor,4);s+=rect(660,550,28,50,'#9B59B6',4);
  [[300,510],[310,535],[900,490],[910,515],[960,500]].forEach(([x,y],i)=>{const col=['#E91E63','#9C27B0','#FF5722'][i%3];s+=circle(x,y,12,col);for(let a=0;a<6;a++){const r=a*Math.PI/3;s+=circle(x+14*Math.cos(r),y+14*Math.sin(r),7,col);}s+=circle(x,y,7,'#F9CA24');});
  for(let i=0;i<antCount;i++){const ax=420+i*70,ay=490+(i%2)*10;s+=circle(ax,ay,5,'#333');s+=circle(ax+8,ay-2,4,'#333');s+=circle(ax-8,ay+3,4,'#333');s+=ln(ax-8,ay,ax+8,ay,'#333',2);}
  return s;
}

// ─── Scene registry ──────────────────────────────────────────────────────────

const SCENES = [
  { id:'park',    title:'공원',   difficulty:1, order:1,
    a:()=>scenepark({}),
    b:()=>scenepark({benchColor:'#3498DB',balloonColors:['#3498DB','#2ECC71','#9B59B6']}),
    diffs:[[0.48,0.65],[0.45,0.46],[0.53,0.43]] },
  { id:'kitchen', title:'주방',   difficulty:2, order:2,
    a:()=>scenekitchen({}),
    b:()=>scenekitchen({curtainColor:'#3498DB',appleCount:2,mugColor:'#E74C3C'}),
    diffs:[[0.13,0.40],[0.22,0.60],[0.79,0.57]] },
  { id:'beach',   title:'해변',   difficulty:3, order:3,
    a:()=>scenebeach({}),
    b:()=>scenebeach({umbrellaColor:'#3498DB',castleTowers:2,boatColor:'#E74C3C'}),
    diffs:[[0.32,0.48],[0.73,0.68],[0.79,0.43]] },
  { id:'forest',  title:'숲속',   difficulty:4, order:4,
    a:()=>sceneforest({}),
    b:()=>sceneforest({mushroomColor:'#9B59B6',deerVisible:false,butterflyVisible:false}),
    diffs:[[0.30,0.68],[0.40,0.66],[0.17,0.65],[0.54,0.45]] },
  { id:'room',    title:'방',     difficulty:5, order:5,
    a:()=>sceneroom({}),
    b:()=>sceneroom({bedColor:'#E74C3C',lampColor:'#27AE60',plantPotColor:'#3498DB',catVisible:false}),
    diffs:[[0.38,0.66],[0.57,0.55],[0.83,0.64],[0.72,0.69]] },
  { id:'street',  title:'거리',   difficulty:6, order:6,
    a:()=>scenestreet({}),
    b:()=>scenestreet({buildingColors:['#27AE60','#E74C3C','#9B59B6','#3498DB'],carColor:'#E74C3C',treeCount:2}),
    diffs:[[0.09,0.40],[0.31,0.35],[0.56,0.38],[0.80,0.35],[0.47,0.64],[0.14,0.55]] },
  { id:'space',   title:'우주',   difficulty:7, order:7,
    a:()=>scenespace({}),
    b:()=>scenespace({planetColor:'#9B59B6',rocketColor:'#E74C3C',moonColor:'#F9CA24',ringColor:'#27AE60'}),
    diffs:[[0.75,0.44],[0.46,0.42],[0.17,0.62],[0.74,0.46]] },
  { id:'aquarium',title:'수족관', difficulty:8, order:8,
    a:()=>sceneaquarium({}),
    b:()=>sceneaquarium({fish1Color:'#9B59B6',coralColor:'#3498DB',bubbleCount:5,chestOpen:true}),
    diffs:[[0.25,0.38],[0.63,0.80],[0.21,0.72],[0.83,0.78]] },
  { id:'market',  title:'시장',   difficulty:9, order:9,
    a:()=>scenemarket({}),
    b:()=>scenemarket({awningColors:['#27AE60','#9B59B6','#E74C3C'],appleCount:3,umbrellaColor:'#3498DB',signText:'SALE'}),
    diffs:[[0.12,0.44],[0.47,0.44],[0.82,0.44],[0.13,0.62],[0.90,0.43],[0.26,0.14]] },
  { id:'picnic',  title:'소풍',   difficulty:10, order:10,
    a:()=>scenepicnic({}),
    b:()=>scenepicnic({blanketColor:'#3498DB',sandwichCount:2,juiceColor:'#E74C3C',antCount:2,kiteVisible:false}),
    diffs:[[0.47,0.73],[0.47,0.70],[0.54,0.72],[0.35,0.63],[0.55,0.71],[0.58,0.22]] },
];

function tapR(d) { return Number((0.09-(d-1)*0.004).toFixed(4)); }

async function main() {
  fs.mkdirSync(path.dirname(META_PATH), { recursive: true });
  const result = [];

  for (const scene of SCENES) {
    console.log(`★${scene.difficulty} ${scene.id} (${scene.title}) — ${scene.diffs.length} diffs`);
    const dir = path.join(OUT_DIR, scene.id);
    fs.mkdirSync(dir, { recursive: true });

    const aBuf = await sharp(Buffer.from(svgDoc(scene.a()))).jpeg({ quality: 93 }).toBuffer();
    const bBuf = await sharp(Buffer.from(svgDoc(scene.b()))).jpeg({ quality: 93 }).toBuffer();
    fs.writeFileSync(path.join(dir,'a.jpg'), aBuf);
    fs.writeFileSync(path.join(dir,'b.jpg'), bBuf);

    const r = tapR(scene.difficulty);
    result.push({
      id: scene.id, title: scene.title,
      difficulty: scene.difficulty, order: scene.order,
      diffs: scene.diffs.map(([x,y])=>({ x:Number(x.toFixed(4)), y:Number(y.toFixed(4)), r })),
    });
  }

  fs.writeFileSync(META_PATH, JSON.stringify(result,null,2)+'\n');
  console.log(`\n✅ ${result.length} stages → ${META_PATH}`);
}

main().catch(e=>{ console.error(e); process.exit(1); });
