# Compilação e dados locais

O repositório público é um pacote de código em desenvolvimento (1.0.0-dev). Não contém a prévia jogável 0.9.2 nem os dados proprietários usados nela.

## Requisitos

- Java 21, Gradle Wrapper e Rust/Cargo estável.
- Windows x64 para executar o mod com a biblioteca nativa atual.
- Dependências locais em `libs/`, obtidas dos distribuidores originais: Mod Menu 11.0.5; Controlify 2.4.4+1.21.1 Fabric e suas dependências em `libs/controlify-deps/`; Simply Swords 1.70.2 para 1.21.1, Architectury 13.0.11, Fabric Language Kotlin 1.12.3 e Fzzy Config 0.7.7 em `libs/swords/`, com bibliotecas internas em `libs/swords/nested/`.

Esses JARs não são redistribuídos neste repositório. O Gradle atual usa arquivos locais e ainda não oferece uma compilação Java independente desses downloads. VulkanMod é somente uma dependência de execução do perfil local, procurada em `../../Research/vulkan/`.

## Núcleo sintético

Na raiz do repositório:

```powershell
cargo build --manifest-path native/Cargo.toml --locked
```

O recurso padrão `fixture-data` usa `native/src/sim/fixture.rs`, com dados sintéticos de desenvolvimento. Isso permite verificar o núcleo sem os arquivos do jogo; não fornece a fidelidade nem todas as ações da prévia local. Os testes que exigem movimentos reais precisam dos dados locais e do recurso `game-data`.

## Integração Java e prévia local

```powershell
.\gradlew.bat build
```

Esse comando compila a integração Java quando as dependências locais estão presentes; não compila Rust nem gera animações automaticamente. O perfil de desenvolvimento contém caminhos para o workspace original em `gradle.properties` e `build.gradle`; ajuste o diretório temporário e o diretório de execução ao seu ambiente.

Para executar uma compilação sintética no Windows, compile o núcleo com `cargo build --manifest-path native/Cargo.toml --locked --release` e copie `elden_steve_core.dll` para `src/main/resources/natives/windows-x86_64/` antes de executar o Gradle. A prévia com dados reais requer ainda `native/src/sim/extracted.rs`, `movement.bin` e a DLL compilada com `--no-default-features --features game-data`. Esses arquivos são gerados localmente e excluídos do Git. Consulte [procedência](PROVENANCE.md) para o pipeline e as ferramentas upstream. O pipeline completo ainda não está automatizado neste pacote público.

Não use o JAR de desenvolvimento como se fosse a versão final 1.0. Os resultados históricos de validação da 0.9.2 estão em [VALIDATION.md](VALIDATION.md).
