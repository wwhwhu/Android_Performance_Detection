import pandas as pd

from options import options


def prepare_log(mulu, filename, feature):
    df_s = pd.read_csv(mulu + filename)



    # MemAvailable,MemFree,nativeHeapFreeSize
    df_s['MemAvailable'] = df_s['MemAvailable'] / df_s['MemTotal']
    df_s['MemFree'] = df_s['MemFree'] / df_s['MemTotal']
    df_s['FrontAPPSystemMemPercent'] = df_s['FrontAPPSystemMem'] / df_s['MemTotal']
    df_s['FrontAPPJavaHeapPercent'] = df_s['FrontAPPJavaHeap'] / df_s['MemTotal']
    df_s['FrontAPPNativeHeapPercent'] = df_s['FrontAPPNativeHeap'] / df_s['MemTotal']
    df_s['FrontAPPCodeMemPercent'] = df_s['FrontAPPCodeMem'] / df_s['MemTotal']
    df_s['FrontAPPStackMemPercent'] = df_s['FrontAPPStackMem'] / df_s['MemTotal']
    df_s['FrontAPPGraphicsMemPercent'] = df_s['FrontAPPGraphicsMem'] / df_s['MemTotal']
    df_s['FrontAPPPrivateOtherMemPercent'] = df_s['FrontAPPPrivateOtherMem'] / df_s['MemTotal']
    df_s['nativeHeapFreeSize'] = df_s['nativeHeapFreeSize'] / df_s['MemTotal']
    df_s['GpuFreq'] = df_s['GpuFreq'].fillna('315000000')
    df_s = df_s.reset_index(drop=True)
    # FrontApp:0——淘宝
    # df_s.loc[df_s['FrontAPP'] == 'com.ss.android.ugc.aweme', 'FrontAPP'] = 0
    df_s.loc[df_s['FrontAPP'] == 'com.miHoYo.ys.mi', 'FrontAPP'] = 0
    # df_s.loc[df_s['FrontAPP'] == 'com.taobao.taobao', 'FrontAPP'] = 0
    # df_s.loc[df_s['FrontApp'] == 'com.miHoYo.ys.mi', 'FrontApp'] = 1
    # df_s = df_s.loc[df_s['FrontAPP'] == 0]

    df_s = df_s.loc[df_s['FPS'] != 0]
    df_s = df_s.loc[df_s['FrontAppCpuUsage'] > 0]
    df_s = df_s.loc[df_s['FrontAppCpuUsage'] < 1]
    df_s['FPS'] = df_s['FPS'] / 60.0
    df_s = df_s.loc[df_s['FrontAppCpuUsage'] > 0]
    df_s = df_s.loc[df_s['FrontAppCpuUsage'] < 1]
    dfs_pre = df_s[
        ['timestamp', 'nativeHeapFreeSize', 'usedNativeMemPercent', 'GPU missed', 'HWC missed',
         'Total missed', 'FrontAPPPrivateOtherMemPercent', 'FrontAPPStackMemPercent', 'FrontAPPSystemMemPercent', 'FrontAPPGraphicsMemPercent',
         'FrontAPPNativeHeapPercent', 'FrontAPPCodeMemPercent', 'FrontAPPJavaHeapPercent',
         'MissedVsync', 'JankyNum', 'batteryTemp', 'thermalStatus', 'MemAvailable',
         'MemFree', 'tempCPU0','tempCPU1','tempCPU2','tempCPU3','tempCPU4','tempCPU5','tempCPU6','tempCPU7',
         'cpu0Freq', 'cpu1Freq','cpu2Freq','cpu3Freq','cpu4Freq','cpu5Freq','cpu6Freq','cpu7Freq',
         'LowMemory', 'gpuUsage', 'GpuFreq', 'cpuUsage', 'FrontAppCpuUsage',
         'wifiRssi', 'wifiSpeed']]
    dfs_pre = dfs_pre.dropna()
    dfs_pre.to_csv(mulu + "data_AfterDeal.csv", index=False)
    dfs_pre = dfs_pre[feature]
    dfs_pre.to_csv(mulu + "/train.csv", index=False)
