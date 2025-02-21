#include "mainwindow.h"
#include "ui_mainwindow.h"
#include "wirecraft.h"

#include <QHostAddress>
#include <QSerialPort>
#include <QTcpSocket>

// ================================
// Setup
// ================================

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    // Set up UI
    ui->setupUi(this);

    // Set up serial port
    serialPort = new QSerialPort(this);
    QObject::connect(serialPort, &QSerialPort::readyRead, this, &MainWindow::serialPort_readFwdToTcp);

    // Set up TCP socket and related events
    tcpSock = new QTcpSocket(this);
    QObject::connect(tcpSock, &QTcpSocket::connected, this, &MainWindow::tcpSock_logConn);
    QObject::connect(tcpSock, &QTcpSocket::disconnected, this, &MainWindow::tcpSock_logDisconn);
    QObject::connect(tcpSock, &QTcpSocket::errorOccurred, this, &MainWindow::tcpSock_logErr);
    QObject::connect(tcpSock, &QTcpSocket::readyRead, this, &MainWindow::tcpSock_readFwdToSerial);
}

MainWindow::~MainWindow()
{
    delete ui;
}

// ================================
// User Interface
// ================================

// Append generic text to log text edit box
void MainWindow::logTextEdit_appendText(QString logStr)
{
    ui->logTextEdit->append(logStr);
}

// ================================
// Serial Port
// ================================

// Configure and open serial connection
void MainWindow::on_serialConnectPushButton_clicked()
{
    // Why doesn't this work when I use a non-hardcoded string??
    // Oh... it was using the label, not the line edit value.  Oops.
    serialPort->setPortName(ui->serialPortLineEdit->text());

    serialPort->setBaudRate(QSerialPort::Baud57600, QSerialPort::AllDirections);
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

// Close serial connection
void MainWindow::on_serialDisconnectPushButton_clicked()
{
    if(serialPort->isOpen())
    {
        serialPort->close();
        logTextEdit_appendText(WCSTR_SERIALCONN_DISCONN);
    }
}

// Read serial data and write it to TCP socket
void MainWindow::serialPort_readFwdToTcp()
{
    const QByteArray serialBuf = serialPort->readAll();
    tcpSock->write(serialBuf, serialBuf.length());
}

// ================================
// Network
// ================================

// Connect to server
void MainWindow::on_serverConnectPushButton_clicked()
{
    tcpSock->abort();

    logTextEdit_appendText(WCSTR_TCPCONN_ATTEMPT + ui->ipAddrLineEdit->text() + ":" + ui->portLineEdit->text());

    QString ipAddr = ui->ipAddrLineEdit->text();
    int port = ui->portLineEdit->text().toInt();

    tcpSock->connectToHost(ipAddr, port);
}

// Disconnect from server
void MainWindow::on_serverDisconnectPushButton_clicked()
{
    tcpSock->disconnectFromHost();
}

// Log message when TCP socket connects to server
void MainWindow::tcpSock_logConn()
{
    logTextEdit_appendText(WCSTR_TCPCONN_SUCCESS);
}

// Log message when TCP socket disconnects from server
void MainWindow::tcpSock_logDisconn()
{
    logTextEdit_appendText(WCSTR_TCPCONN_DISCONN);
}

// Log error when TCP connection fails
void MainWindow::tcpSock_logErr(QAbstractSocket::SocketError sockErr)
{
    switch(sockErr)
    {
        case QAbstractSocket::ConnectionRefusedError:
            logTextEdit_appendText(WCSTR_TCPERR_REFUSED);
            break;

        case QAbstractSocket::HostNotFoundError:
            logTextEdit_appendText(WCSTR_TCPERR_NOTFOUND);
            break;

        case QAbstractSocket::RemoteHostClosedError:
            logTextEdit_appendText(WCSTR_TCPERR_HOSTCLOSE);
            break;

        default:
            logTextEdit_appendText(WCSTR_TCPERR_GENERIC);
            break;
    }
}

// Read TCP data and write it to serial port
void MainWindow::tcpSock_readFwdToSerial()
{
    const QByteArray tcpBuf = tcpSock->readAll();
    serialPort->write(tcpBuf, tcpBuf.length());
}
