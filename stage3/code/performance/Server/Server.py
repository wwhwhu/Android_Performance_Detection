import re
import socket
import time
import sys
import threading
from joblib import load


def res(i):
    if i < 0:
        return
    elif i == 0:
        return "正常"
    else:
        return "异常"


def deal(msg):
    res0 = -1
    res1 = -1
    res2 = -1
    str1 = re.search(r'com([a-zA-Z]|\.|\d)+', msg)
    print("前台应用：", str1.group())
    lst = [float(x) for x in msg[msg.find('[') + 1:msg.find(']')].split(',')]
    # 将转换后的列表用joblib加载成决策树使用的输入格式
    X = [lst]
    if len(X) < 1:
        return
    if (str1.group() == 'com.taobao.taobao'):
        model1 = load('taobao/cpuDecisionTreeClassifier.pkl')
        res0 = int(model1.predict(X))
        model2 = load('taobao/gpuDecisionTreeClassifier.pkl')
        res1 = int(model2.predict(X))
        model3 = load('taobao/memDecisionTreeClassifier.pkl')
        res2 = int(model3.predict(X))
    elif (str1.group() == 'com.ss.android.ugc.aweme'):
        model1 = load('douyin/cpuDecisionTreeClassifier.pkl')
        res0 = int(model1.predict(X))
        model2 = load('douyin/gpuDecisionTreeClassifier.pkl')
        res1 = int(model2.predict(X))
        model3 = load('douyin/memDecisionTreeClassifier.pkl')
        res2 = int(model3.predict(X))
    elif (str1.group() == 'com.miHoYo.ys.mi'):
        model1 = load('yuanshen/cpuDecisionTreeClassifier.pkl')
        res0 = int(model1.predict(X))
        model2 = load('yuanshen/gpuDecisionTreeClassifier.pkl')
        res1 = int(model2.predict(X))
        model3 = load('yausheng/memDecisionTreeClassifier.pkl')
        res2 = int(model3.predict(X))
    else:
        model1 = load('entire/cpuDecisionTreeClassifier.pkl')
        res0 = int(model1.predict(X))
        model2 = load('entire/gpuDecisionTreeClassifier.pkl')
        res1 = int(model2.predict(X))
        model3 = load('entire/memDecisionTreeClassifier.pkl')
        res2 = int(model3.predict(X))
    print("异常检测结果是：" + "CPU异常检测结果" + str(res0) + res(res0) + "GPU异常检测结果" + str(res1) + res(res1) + "内存异常检测结果" + str(res2) + res(res2))
    return "异常检测结果是：" + str(res0) + str(res1) + str(res2)


def server():
    # 获取本机电脑名
    myname = socket.gethostname()
    # 获取本机ip
    myPC_IP = socket.gethostbyname(myname)

    COD = 'utf-8'
    # 需要内网IP
    HOST = myPC_IP  # 主机ip
    # 服务器防火墙需要打开这个端口，允许访问
    PORT = 6006  # 软件端口号
    BUFSIZ = 1024
    ADDR = (HOST, PORT)
    SIZE = 50
    tcpS = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # 创建socket对象
    print("Server: 连接中Connecting...")
    tcpS.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # 加入socket配置，重用ip和端口
    tcpS.bind(ADDR)  # 绑定ip端口号
    tcpS.listen(SIZE)  # 设置最大链接数
    while True:
        msg = ""
        clientsocket, addr = tcpS.accept()
        print("Server: 接收中...Receiving From IP,PORT:", addr, "...")
        print("Server: 准备读取")
        while True:
            try:
                data = clientsocket.recv(BUFSIZ)  # 读取已链接客户的发送的消息
            except Exception:
                print("Server: 客户端连接断开Error1...", addr)
                break
            msg += data.decode(COD)
            if len(data) < BUFSIZ:
                print("Server: 读取完毕")
                break
        if len(msg) > 0:
            print("Server: 客户端发送的内容:", msg)
            fore = deal(msg)
            try:
                clientsocket.send((
                        "Response from ip:" + HOST + ". Server has already received the information from client. The Total missed is " + fore).encode(
                    COD))  # 发送消息给已链接客户端
                print("Server: 成功回复客户端...", addr)
            except:
                print("Server: 客户端连接异常Error2...", addr)
        else:
            print("未接收任何信息Not receiver anything from client!", msg)
        clientsocket.close()  # 关闭客户端链接
        print("Server: Done.\n\n\n")
    tcpS.close()


thread = threading.Thread(target=server, name="ModelServer")
thread.start()
