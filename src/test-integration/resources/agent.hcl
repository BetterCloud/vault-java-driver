pid_file = "/tmp/agent_pidfile"

vault {
  address = "https://vault:8200"
}
auto_auth {
  method {
    type = "approle"
    config = {
      role_id_file_path = "/home/vault/role_id"
      secret_id_file_path = "/home/vault/secret_id"
    }
  }
  sink {
    type = "file"
    config = {
      path = "/tmp/file-foo"
    }
  }
}
cache {
  use_auto_auth_token = true
}
listener "tcp" {
  address = "0.0.0.0:8100"
  tls_disable = true
}
