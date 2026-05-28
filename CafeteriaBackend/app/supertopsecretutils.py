import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import psycopg2
from psycopg2.extras import RealDictCursor

def enviar_correo_pedido_listo(pedido_id: str):
    """
    Busca el usuario asociado al pedido_id, obtiene su correo 
    y le envía una notificación SMTP de forma asíncrona.
    """
    conn = None
    try:
        conn = psycopg2.connect(os.getenv("DATABASE_URL"))
        cursor = conn.cursor(cursor_factory=RealDictCursor)

        # 1. Obtener el usuario_id desde el pedido
        query_pedido = "SELECT usuario_id FROM public.pedidos WHERE pedido_id = %s;"
        cursor.execute(query_pedido, (pedido_id,))
        pedido = cursor.fetchone()
        
        if not pedido:
            print(f"⚠️ No se encontró el pedido {pedido_id} para enviar correo.")
            return

        # 2. Obtener el nombre y correo del alumno/usuario
        query_usuario = "SELECT nombre, correo FROM public.usuarios WHERE id = %s;"
        cursor.execute(query_usuario, (pedido["usuario_id"],))
        usuario = cursor.fetchone()

        if not usuario or not usuario.get("correo"):
            print(f"⚠️ El usuario {pedido['usuario_id']} no tiene un correo válido registrado.")
            return

        # 3. Configuración de credenciales desde variables de entorno
        remitente = os.getenv("SMTP_USER")
        password = os.getenv("SMTP_PASSWORD")
        destinatario = usuario["correo"]

        if not remitente or not password:
            print("Error: SMTP_USER o SMTP_PASSWORD no configurados en el entorno.")
            return

        # 4. Construcción del cuerpo del correo (MIME)
        msg = MIMEMultipart()
        msg['From'] = remitente
        msg['To'] = destinatario
        msg['Subject'] = "☕ ¡Tu pedido de la Cafetería está LISTO!"

        cuerpo = f"""
        Hola {usuario['nombre']},
        
        ¡Tu orden ya fue preparada por el barista y está lista para que pases a recogerla! 
        
        Por favor, dirígete a la barra de la cafetería y muestra tu pantalla de estado en la aplicación.
        
        ¡Que disfrutes tu bebida!
        """
        msg.attach(MIMEText(cuerpo, 'plain', 'utf-8'))

        # 5. Conexión segura con los servidores de Google (Puerto 587 TLS)
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.starttls()  # Cifrado seguro
        server.login(remitente, password)
        server.sendmail(remitente, destinatario, msg.as_string())
        server.quit()
        
        print(f"📧 Correo enviado con éxito a {destinatario} para el pedido {pedido_id}")

    except Exception as e:
        print(f"❌ Error en el proceso de envío de correo: {str(e)}")
    finally:
        if conn:
            conn.close()