// Fixed cinematic backdrop: dark circuit traces with red data pulses running along them, an LED dot field
// and a slow scan line. Decorative only; all motion lives in cinema.css and stops under reduced motion.
const TRACES = [
  "M-20 120 H260 L320 180 H640 L700 120 H1460",
  "M-20 420 H180 L240 360 H520 L580 420 H900 L960 480 H1460",
  "M-20 720 H360 L420 660 H780 L840 720 H1460",
  "M1100 -20 V200 L1160 260 V560 L1100 620 V920",
  "M200 -20 V140 L260 200 V520 L200 580 V920",
  "M-20 880 H520 L580 820 H1000 L1060 880 H1460",
];
const NODES = [[260, 120], [700, 120], [240, 360], [580, 420], [420, 660], [840, 720], [1160, 260], [260, 200], [580, 820]];

export function BackdropFX() {
  return (
    <div className="fx-bg" aria-hidden>
      <div className="fx-led" />
      <svg viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice">
        {TRACES.map((d) => <path key={d} className="fx-trace" d={d} />)}
        {TRACES.map((d) => <path key={`p${d}`} className="fx-pulse" d={d} />)}
        {NODES.map(([x, y]) => <rect key={`${x}-${y}`} className="fx-node" x={x - 3} y={y - 3} width="6" height="6" />)}
      </svg>
      <div className="fx-scan" />
      <div className="fx-vignette" />
    </div>
  );
}
