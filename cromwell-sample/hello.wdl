task hello {
  String name

  command {
    echo 'Hello ${name}!'
  }

  output {
    File response = stdout()
  }

  runtime {
    docker: "ubuntu:latest"
  }
}

workflow test {
  call hello
}