(() => {
  const btn = document.getElementById("copyCmd");
  if (!btn) return;

  const cmd = "javac PageReplacementSimulator.java && java PageReplacementSimulator";

  btn.addEventListener("click", async () => {
    const original = btn.textContent;
    try {
      await navigator.clipboard.writeText(cmd);
      btn.textContent = "Copied!";
    } catch {
      btn.textContent = "Copy failed";
    } finally {
      window.setTimeout(() => {
        btn.textContent = original;
      }, 1200);
    }
  });
})();

