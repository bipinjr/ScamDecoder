# DECODR

Checks any suspicious message, voice note, screenshot or QR code, says whether it is a scam, and explains every trick in the reader's own language. One check service sits behind the web app, the browser extension, a Telegram bot and a WhatsApp bot.

## Run

```bash
npm install
cp .env.example .env.local   # then fill in the keys
npm run dev                  # http://localhost:3000
```

Run `supabase/schema.sql` once in the Supabase SQL editor for the Scam Radar.

## Telegram bot

1. Create a bot with @BotFather and put its token in `TELEGRAM_BOT_TOKEN`.
2. Put any long random string in `TELEGRAM_WEBHOOK_SECRET`.
3. Deploy, then register the webhook once (replace the three values):

```bash
curl "https://api.telegram.org/bot<TOKEN>/setWebhook" -d "url=https://<your-domain>/api/telegram" -d "secret_token=<SECRET>"
```

Forward the bot a message, a screenshot or a voice note.

## WhatsApp bot (Twilio)

1. Put `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN` in the environment.
2. In the Twilio console, WhatsApp sandbox settings, set "When a message comes in" to `https://<your-domain>/api/whatsapp` (POST).
3. Each tester sends the sandbox join code to the sandbox number once.

Requests without a valid Telegram secret or Twilio signature are refused.

## Checks

```bash
node --experimental-strip-types scripts/check-bot.mts
node --experimental-strip-types scripts/check-signals.mts
```
