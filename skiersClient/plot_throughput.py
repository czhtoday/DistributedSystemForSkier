import pandas as pd
import matplotlib.pyplot as plt

csv_path = "/Users/zhihang/Documents/CS6650Assignments/skiers-client/request_log.csv"
df = pd.read_csv(csv_path, skiprows=1, header=None, names=["StartTime", "RequestType", "Latency", "ResponseCode"], dtype={"StartTime": str, "Latency": str}, low_memory=False)

df = df[df["StartTime"].str.isnumeric() & df["Latency"].str.isnumeric()]
df["StartTime"] = df["StartTime"].astype(int)
df["Latency"] = df["Latency"].astype(int)

df['StartTime'] = (df['StartTime'] - df['StartTime'].min()) / 1000
df['TimeWindow'] = df['StartTime'].astype(int)
throughput_over_time = df.groupby('TimeWindow').size()

plt.figure(figsize=(10, 5))
plt.plot(throughput_over_time.index, throughput_over_time.values, marker='o', linestyle='-')
plt.xlabel("Time (seconds)")
plt.ylabel("Requests per Second")
plt.title("Throughput Over Time")
plt.grid()
plt.savefig("throughput_plot.png")

