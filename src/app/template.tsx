// A template remounts on every navigation (a layout does not), so each new page gets the fade-in.
export default function Template({ children }: { children: React.ReactNode }) {
  return <div className="page-in">{children}</div>;
}
