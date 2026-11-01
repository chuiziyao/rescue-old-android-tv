import sys
import threading
import urllib.request

url = sys.argv[1]
out = sys.argv[2]
threads = int(sys.argv[3]) if len(sys.argv) > 3 else 8

req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, timeout=30) as r:
    total = int(r.headers["Content-Length"])
print("total", total, "threads", threads, flush=True)

chunk = (total + threads - 1) // threads
locks = [threading.Lock()]
progress = [0]


def fetch(i, start, end):
    hdrs = {"Range": "bytes=%d-%d" % (start, end), "User-Agent": "Mozilla/5.0"}
    for attempt in range(8):
        try:
            req = urllib.request.Request(url, headers=hdrs)
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if len(data) != end - start + 1:
                raise IOError("short read %d != %d" % (len(data), end - start + 1))
            with open(out, "r+b") as f:
                f.seek(start)
                f.write(data)
            with locks[0]:
                progress[0] += len(data)
                print("progress", progress[0], "/", total, flush=True)
            return
        except Exception as e:
            print("retry", i, attempt, e, flush=True)
    raise SystemExit("chunk %d failed" % i)


with open(out, "wb") as f:
    f.truncate(total)

ts = []
for i in range(threads):
    start = i * chunk
    end = min(total - 1, (i + 1) * chunk - 1)
    t = threading.Thread(target=fetch, args=(i, start, end))
    t.start()
    ts.append(t)
for t in ts:
    t.join()
print("done", out, flush=True)
