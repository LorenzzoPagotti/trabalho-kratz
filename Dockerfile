# ------------------------------------------------------------
# Jogo de Adivinhação (Java Socket TCP) - Servidor em Docker
# Grupo: Lorenzzo Freitas Pagotti e Kauã Vieira Porto
# ------------------------------------------------------------

# 1) Imagem base Java (JDK 17), compatível com o projeto (Java 8+)
FROM eclipse-temurin:17-jdk

# Diretório de trabalho dentro do contêiner
WORKDIR /app

# 2) Copia o código-fonte do projeto para o contêiner
COPY Servidor.java Cliente.java ./

# 3) Compila a aplicação Java dentro da imagem
RUN javac -encoding UTF-8 Servidor.java Cliente.java

# 4) Porta TCP utilizada pelo servidor do jogo
EXPOSE 5000

# 5) Inicialização automática do servidor quando o contêiner sobe
CMD ["java", "-Dfile.encoding=UTF-8", "Servidor"]
