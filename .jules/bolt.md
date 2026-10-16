## 2024-10-16 - Protobuf Socket Parsing I/O
**Learning:** Parsing delimited protobuf messages from a raw socket causes severe I/O overhead due to byte-by-byte varint length prefix reading on an unbuffered stream, translating to numerous inefficient system calls.
**Action:** Always wrap `InputStream` in `.buffered()` when reading from raw sockets or doing fine-grained byte parsing. Avoid buffering `OutputStream` blindly on interactive connections without explicitly ensuring `.flush()` is called, to avoid hangs.
