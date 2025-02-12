#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
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
        QTcpSocket *tcpSock;    // TCP socket
        QByteArray tcpBuf;     // TCP buffer

    public slots:
        void logTextEdit_appendText(QString logStr);
        void on_connectPushButton_clicked();
        void on_disconnectPushButton_clicked();
        void tcpSock_logConn();
        void tcpSock_logDisconn();
        void tcpSock_logErr(QAbstractSocket::SocketError sockErr);
        void tcpSock_read();
};

#endif // MAINWINDOW_H
