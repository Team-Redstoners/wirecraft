#include "mainwindow.h"
#include "ui_mainwindow.h"

#include <QHostAddress>
#include <QTcpSocket>

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    // Set up TCP socket and related events
    tcpSock = new QTcpSocket(this);
    QObject::connect(tcpSock, &QTcpSocket::connected, this, &MainWindow::tcpSock_logConn);
    QObject::connect(tcpSock, &QTcpSocket::disconnected, this, &MainWindow::tcpSock_logDisconn);
    QObject::connect(tcpSock, &QTcpSocket::errorOccurred, this, &MainWindow::tcpSock_logErr);
    QObject::connect(tcpSock, &QTcpSocket::readyRead, this, &MainWindow::tcpSock_read);

    // Set up UI
    ui->setupUi(this);
}

MainWindow::~MainWindow()
{
    delete ui;
}

// Append generic text to log text edit box
void MainWindow::logTextEdit_appendText(QString logStr)
{
    ui->logTextEdit->append(logStr);
}

// Connect to server
void MainWindow::on_connectPushButton_clicked()
{
    tcpSock->abort();

    logTextEdit_appendText("Attempting to connect to server...");

    QString ipAddr = ui->ipAddrLineEdit->text();
    int port = ui->portLineEdit->text().toInt();

    tcpSock->connectToHost(ipAddr, port);
}

// Disconnect from server
void MainWindow::on_disconnectPushButton_clicked()
{
    tcpSock->disconnectFromHost();
}

// Log message when TCP socket connects to server
void MainWindow::tcpSock_logConn()
{
    logTextEdit_appendText("Successfully connected to server!");
    tcpSock->write("Wirecraft Client Application");
}

// Log message when TCP socket disconnects from server
void MainWindow::tcpSock_logDisconn()
{
    logTextEdit_appendText("Disconnected from server!");
}

// Log error when TCP connection fails
void MainWindow::tcpSock_logErr(QAbstractSocket::SocketError sockErr)
{
    switch(sockErr)
    {
        case QAbstractSocket::ConnectionRefusedError:
            logTextEdit_appendText("Connection refused by host. Please verify that server is running and port # entered is correct.");
            break;

        case QAbstractSocket::HostNotFoundError:
            logTextEdit_appendText("Host not found. Please verify that server address and port # entered are correct.");
            break;

        case QAbstractSocket::RemoteHostClosedError:
            logTextEdit_appendText("Connection closed by host.");
            break;

        default:
            logTextEdit_appendText("Unable to connect.");
            break;
    }
}

// Read TCP data and output it to the logs
void MainWindow::tcpSock_read()
{
    tcpBuf = tcpSock->readAll();
    logTextEdit_appendText(tcpBuf);
}
