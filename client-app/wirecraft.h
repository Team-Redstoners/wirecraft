#ifndef WIRECRAFT_H
#define WIRECRAFT_H

// Keep to show incoming/outgoing bytes in the logs
#define DEBUG_ENABLE

// String Constants
#define WCSTR_BULLET                "* "
#define WCSTR_SERIALCONN_CONN       WCSTR_BULLET "Successfully connected to serial port: "
#define WCSTR_SERIALCONN_DISCONN    WCSTR_BULLET "Disconnected from serial port."
#define WCSTR_SERIALERR             WCSTR_BULLET "Unable to connect to serial port: "
#define WCSTR_SERVER_CLOSE          WCSTR_BULLET "Closed server."
#define WCSTR_SERVER_START          WCSTR_BULLET "Successfully started server with host address "
#define WCSTR_SVCONN_CONNECTED      WCSTR_BULLET "Incoming connection was successful."
#define WCSTR_SVCONN_INCOMING       WCSTR_BULLET "Received incoming user connection with address "
#define WCSTR_TCPERR_DISCONN        WCSTR_BULLET "User disconnected."
#define WCSTR_TCPERR_UNKNOWN        WCSTR_BULLET "An unknown error occurred."
#define WCSTR_TCPSOCK_CLOSED        WCSTR_BULLET "Closed user connection."
#define WCSTR_TCPSOCK_INVALID       WCSTR_BULLET "TCP socket is not valid."

#endif // WIRECRAFT_H
