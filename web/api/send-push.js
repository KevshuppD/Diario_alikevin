const { GoogleAuth } = require('google-auth-library');

module.exports = async function handler(req, res) {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, error: 'Method not allowed' });
  }

  const { title, body, topic, token, data = {} } = req.body || {};

  if (!title && !data.click_type) {
    return res.status(400).json({ success: false, error: 'Parámetros insuficientes para la notificación' });
  }

  const targetTopic = topic || 'diario_vinculo_unico_123';
  const rawSa = process.env.FIREBASE_SERVICE_ACCOUNT || process.env.SERVICE_ACCOUNT_JSON;

  if (!rawSa) {
    return res.status(500).json({
      success: false,
      error: 'FIREBASE_SERVICE_ACCOUNT no configurado en las variables de entorno de Vercel'
    });
  }

  try {
    const credentials = typeof rawSa === 'string' ? JSON.parse(rawSa) : rawSa;
    const projectId = credentials.project_id || 'diario-ali-kevin';

    const auth = new GoogleAuth({
      credentials,
      scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
    });

    const client = await auth.getClient();
    const accessTokenResponse = await client.getAccessToken();
    const accessToken = accessTokenResponse.token;

    if (!accessToken) {
      throw new Error('No se pudo generar el access token de Firebase OAuth2');
    }

    const messagePayload = {
      message: {
        ...(token ? { token } : { topic: targetTopic }),
        notification: title ? { title, body: body || '' } : undefined,
        data: Object.fromEntries(
          Object.entries({
            title: title || '',
            body: body || '',
            ...data,
          }).map(([k, v]) => [k, String(v)])
        ),
        android: {
          priority: 'HIGH',
          notification: title
            ? {
                channel_id: 'diario_channel',
                sound: 'default',
                notification_priority: 'PRIORITY_HIGH',
              }
            : undefined,
        },
      },
    };

    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(messagePayload),
      }
    );

    const resData = await response.json();

    if (!response.ok) {
      console.error('❌ Error FCM v1:', resData);
      return res.status(response.status).json({ success: false, error: resData });
    }

    return res.status(200).json({ success: true, messageId: resData.name });
  } catch (error) {
    console.error('❌ Error enviando push desde Vercel:', error);
    return res.status(500).json({ success: false, error: error.message });
  }
};
