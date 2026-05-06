# SSL 证书目录

按数据库类型存放 SSL 认证文件，本目录的证书文件不提交 Git。

```
certs/
├── README.md        # 本文件
├── mysql/
│   ├── .gitkeep     # 占位文件（目录结构提交 Git）
│   ├── ca.pem        # CA 证书
│   ├── cert.pem      # 客户端证书
│   └── key.pem       # 客户端私钥
├── postgresql/
│   └── .gitkeep
├── oracle/
│   └── .gitkeep
└── h2/
    └── .gitkeep      # H2 通常不需要 SSL
```

在 `db-connection.json` 的 `ssl` 字段中配置证书路径后，Java 客户端自动启用 SSL。
