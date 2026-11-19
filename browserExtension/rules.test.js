// node browserExtension/rules.test.js
const assert = require("assert");
const { blockReason } = require("./rules.js");

assert.strictEqual(blockReason("https://www.tiktok.com/@x", false), "always");
assert.strictEqual(blockReason("https://instagram.com/", false), "always");
assert.strictEqual(blockReason("https://www.youtube.com/watch?v=1", false), null);
assert.strictEqual(blockReason("https://www.youtube.com/watch?v=1", true), "focus");
assert.strictEqual(blockReason("https://m.youtube.com/", true), "focus");
assert.strictEqual(blockReason("https://old.reddit.com/r/x", true), "focus");
assert.strictEqual(blockReason("https://discord.com/channels/1", true), "focus");
assert.strictEqual(blockReason("https://www.twitch.tv/x", true), "focus");
// No confundir dominios parecidos ni otras webs.
assert.strictEqual(blockReason("https://notyoutube.com/", true), null);
assert.strictEqual(blockReason("https://github.com/", true), null);
assert.strictEqual(blockReason("https://youtube.com.evil.example/", true), null);
assert.strictEqual(blockReason("about:blank", true), null);
console.log("rules: ok");
