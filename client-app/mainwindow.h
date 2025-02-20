#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QSerialPort>
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
        QTcpSocket *tcpSock;

    public slots:
        void logTextEdit_appendText(QString logStr);
        void on_serialConnectPushButton_clicked();
        void on_serialDisconnectPushButton_clicked();
        void on_serverConnectPushButton_clicked();
        void on_serverDisconnectPushButton_clicked();
        void serialPort_readFwdToTcp();
        void tcpSock_logConn();
        void tcpSock_logDisconn();
        void tcpSock_logErr(QAbstractSocket::SocketError sockErr);
        void tcpSock_readFwdToSerial();
};

#endif // MAINWINDOW_H
