#include "mainwindow.h"
#include "ui_mainwindow.h"
#include "wirecraft.h"

#include <QHostAddress>
#include <QSerialPort>
#include <QTcpSocket>
#include <QtNetwork>

// ================================
// Setup
// ================================

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    // Set up UI.
    ui->setupUi(this);

    // Set up serial port.
    serialPort = new QSerialPort(this);
    QObject::connect(serialPort, &QSerialPort::readyRead, this, &MainWindow::serialPort_readFwdToTcp);

    // Set up TCP socket
    //
    // NOTE: This needs to be here.
    // If not, the app will crash if the "Close Server" button is pressed before establishing a connection.
    tcpSock = new QTcpSocket(this);

    // Set up TCP server and related events.
    tcpServer = new QTcpServer(this);
    QObject::connect(tcpServer, &QTcpServer::acceptError, this, &MainWindow::tcpSock_logErr);
    QObject::connect(tcpServer, &QTcpServer::newConnection, this, &MainWindow::tcpServer_handleConn);
}

MainWindow::~MainWindow()
{
    delete ui;
}

// ================================
// User Interface
// ================================

// Append generic text to log text edit box.
void MainWindow::logTextEdit_appendText(QString logStr)
{
    ui->logTextEdit->append(logStr);
}

// ================================
// Serial Port
// ================================

// Configure and open serial connection.
void MainWindow::on_serialConnectPushButton_clicked()
{
    serialPort->setPortName(ui->serialPortLineEdit->text());
    serialPort->setBaudRate(QSerialPort::Baud115200, QSerialPort::AllDirections);
    serialPort->setDataBits(QSerialPort::UnknownDataBits);
    serialPort->setParity(QSerialPort::NoParity);
    serialPort->setStopBits(QSerialPort::UnknownStopBits);
    serialPort->setFlowControl(QSerialPort::UnknownFlowControl);

    if(serialPort->open(QIODevice::ReadWrite))
    {
        logTextEdit_appendText(WCSTR_SERIALCONN_CONN + ui->serialPortLineEdit->text());
    }
    else
    {
        logTextEdit_appendText(WCSTR_SERIALERR + ui->serialPortLineEdit->text());
    }
}

// Close serial connection.
void MainWindow::on_serialDisconnectPushButton_clicked()
{
    if(serialPort->isOpen())
    {
        serialPort->close();
        logTextEdit_appendText(WCSTR_SERIALCONN_DISCONN);
    }
}

// Read serial data and write it to TCP socket.
void MainWindow::serialPort_readFwdToTcp()
{
    const QByteArray serialBuf = serialPort->readAll().simplified();
    tcpSock->write(serialBuf, serialBuf.length());

#ifdef DEBUG_ENABLE
    logTextEdit_appendText("[Serial->TCP] " + serialBuf.toHex());
#endif
}

// ================================
// Network
// ================================

// Close the client app server.
void MainWindow::on_closeServerPushButton_clicked()
{
    if(tcpSock->isValid())
    {
        tcpSock->close();
        logTextEdit_appendText(WCSTR_TCPSOCK_CLOSED);
    }

    if(tcpServer->isListening())
    {
        tcpServer->close();
        logTextEdit_appendText(WCSTR_SERVER_CLOSE);
    }
}

// Start the client app server.
void MainWindow::on_startServerPushButton_clicked()
{
    // The following code was pulled almost directly from the following example:
    // https://code.qt.io/cgit/qt/qtbase.git/tree/examples/network/fortuneserver/server.cpp?h=6.8

    tcpServer->listen(QHostAddress::Any, ui->serverPortLineEdit->text().toInt());

    QString ipAddr;
    const QList<QHostAddress> ipAddrList = QNetworkInterface::allAddresses();

    // Set host address to first non-localhost address.
    for(const QHostAddress &hostAddr : ipAddrList)
    {
        if(hostAddr != QHostAddress::LocalHost && hostAddr.toIPv4Address())
        {
            ipAddr = hostAddr.toString();
            break;
        }
    }

    // Otherwise, use localhost address.
    if(ipAddr.isEmpty())
    {
        ipAddr = QHostAddress(QHostAddress::LocalHost).toString();
    }

    logTextEdit_appendText(WCSTR_SERVER_START + ipAddr + ":" + ui->serverPortLineEdit->text() + ".");
}

// Handle incoming connections to client app server.
void MainWindow::tcpServer_handleConn()
{
    // Set TCP socket to next incoming connection.
    tcpSock = tcpServer->nextPendingConnection();
    logTextEdit_appendText(WCSTR_SVCONN_INCOMING + tcpSock->peerAddress().toString() + ".");

    if(tcpSock->isValid())
    {
        logTextEdit_appendText(WCSTR_SVCONN_CONNECTED);

        // Set up TCP socket and related events.
        QObject::connect(tcpSock, &QTcpSocket::errorOccurred, this, &MainWindow::tcpSock_logErr);
        QObject::connect(tcpSock, &QTcpSocket::readyRead, this, &MainWindow::tcpSock_readFwdToSerial);
    }
    else
    {
        logTextEdit_appendText(WCSTR_TCPSOCK_INVALID);
    }
}

// Log error when TCP server connection fails
void MainWindow::tcpServer_logErr(QAbstractSocket::SocketError sockErr)
{
    switch(sockErr)
    {
        default:
            logTextEdit_appendText(WCSTR_TCPERR_UNKNOWN);
            break;
    }
}

// Log error when TCP socket connection fails.
void MainWindow::tcpSock_logErr(QAbstractSocket::SocketError sockErr)
{
    switch(sockErr)
    {
        case QAbstractSocket::RemoteHostClosedError:
            logTextEdit_appendText(WCSTR_TCPERR_DISCONN);
            break;

        default:
            logTextEdit_appendText(WCSTR_TCPERR_UNKNOWN);
            break;
    }
}

// Read TCP data and write it to serial port.
void MainWindow::tcpSock_readFwdToSerial()
{
    const QByteArray tcpBuf = tcpSock->readAll().simplified();
    serialPort->write(tcpBuf, tcpBuf.length());

#ifdef DEBUG_ENABLE
    logTextEdit_appendText("[TCP->Serial] " + tcpBuf.toHex());
#endif
}
