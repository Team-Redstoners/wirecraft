#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QSerialPort>
#include <QTcpServer>
#include <QTcpSocket>

QT_BEGIN_NAMESPACE
namespace Ui { class MainWindow; }
QT_END_NAMESPACE

class MainWindow : public QMainWindow
{
    Q_OBJECT

    public:
        MainWindow(QWidget *parent = nullptr);
        ~MainWindow();

    private:
        Ui::MainWindow *ui;
        QSerialPort *serialPort;
        QTcpServer *tcpServer;
        QTcpSocket *tcpSock;

    public slots:
        void logTextEdit_appendText(QString logStr);
        void on_serialConnectPushButton_clicked();
        void on_serialDisconnectPushButton_clicked();
        void on_closeServerPushButton_clicked();
        void on_startServerPushButton_clicked();
        void serialPort_readFwdToTcp();
        void tcpServer_handleConn();
        void tcpServer_logErr(QAbstractSocket::SocketError sockErr);
        void tcpSock_logErr(QAbstractSocket::SocketError sockErr);
        void tcpSock_readFwdToSerial();
};

#endif // MAINWINDOW_H
