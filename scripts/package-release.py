#!/usr/bin/env python3
"""Package the real Maven output, never Minecraft media or a placeholder binary."""
import hashlib
import io
import os
from pathlib import Path
import sys
import zipfile

ROOT = Path(__file__).resolve().parent.parent
VERSION = '0.10.0'
JAR = ROOT / 'target' / f'alpha-desktop-{VERSION}.jar'


def verify():
    if not JAR.is_file():
        raise SystemExit(f'Maven executable missing: {JAR}')
    with zipfile.ZipFile(JAR) as jar:
        names = set(jar.namelist())
        required = {'AlphaDesktop.class', 'DirectAlphaLaunch.class', 'LauncherHome.class', 'LaunchSession.class', 'StartupRecovery.class', 'StartupDiagnostics.class', 'LegacyRuntime.class', 'AlphaAgentSupport.class', 'OriginalEngineSetup.class', 'OriginalAlphaInstallation.class', 'LauncherJson.class', 'CameraRays.class', 'DesktopSetup.class', 'WindowMode.class', 'AlphaPresentation.class', 'OriginalAudio.class', 'VoxelWorld.class',
                    'javazoom/spi/vorbis/sampled/file/VorbisAudioFileReader.class',
                    'META-INF/services/javax.sound.sampled.spi.AudioFileReader',
                    'META-INF/services/javax.sound.sampled.spi.FormatConversionProvider'}
        missing = required - names
        if missing:
            raise SystemExit(f'Executable incomplete: {sorted(missing)}')
        if 'Main-Class: AlphaDesktop' not in jar.read('META-INF/MANIFEST.MF').decode():
            raise SystemExit('Executable manifest has no AlphaDesktop entry point')
        for service in ('AudioFileReader', 'FormatConversionProvider'):
            contents = jar.read('META-INF/services/javax.sound.sampled.spi.' + service).decode()
            if 'vorbis' not in contents.lower():
                raise SystemExit(f'Original Ogg decoder not registered for {service}')
        if any(name.lower().endswith(('.png', '.ogg', '.wav')) for name in names):
            raise SystemExit('Unexpected media bundled into the application')
        agent_name = f'mods/alpha-ambient-agent-{VERSION}.jar'
        if agent_name not in names:
            raise SystemExit('Optional original-engine mod was not embedded; build agent/pom.xml first')
        with zipfile.ZipFile(io.BytesIO(jar.read(agent_name))) as agent:
            if 'Premain-Class: alpha.ambient.AmbientAgent' not in agent.read('META-INF/MANIFEST.MF').decode():
                raise SystemExit('Runtime mod manifest is incomplete')
            agent_names = set(agent.namelist())
            if 'alpha/ambient/OfflinePatcher.class' not in agent_names:
                raise SystemExit('Install-time offline patcher missing')
            if 'Main-Class: alpha.ambient.AgentProbe' not in agent.read('META-INF/MANIFEST.MF').decode():
                raise SystemExit('Agent preflight entry point missing')
            if any(name.startswith('org/objectweb/asm/') for name in agent_names):
                raise SystemExit('ASM was not relocated out of the game library namespace')
            if 'alpha/ambient/internal/asm/ClassReader.class' not in agent_names:
                raise SystemExit('Private instrumenter dependency missing')
            with zipfile.ZipFile(io.BytesIO(agent.read('alpha/ambient/bootstrap-hooks.jar'))) as hooks:
                if 'alpha/ambient/AmbientHooksDirector.class' not in hooks.namelist():
                    raise SystemExit('Runtime encounter director missing from hook bundle')
                for name in hooks.namelist():
                    if name.endswith('.class') and not name.startswith('alpha/ambient/AmbientHooks'):
                        raise SystemExit('Unexpected class in isolated bootstrap helper')
            for member in agent.namelist():
                if member.startswith('alpha/ambient/') and member.endswith('.class'):
                    if int.from_bytes(agent.read(member)[6:8], 'big') > 52:
                        raise SystemExit('Runtime mod is not Java 8 compatible')
                if member.lower().endswith(('.png', '.ogg', '.wav')):
                    raise SystemExit('Unexpected media in optional mod')
    print('Verified executable manifest, native game classes, Vorbis decoder and SPI services.')


def package():
    verify()
    output = ROOT / 'release'
    output.mkdir(exist_ok=True)
    archive = output / f'alpha-desktop-{VERSION}.zip'
    prefix = f'alpha-desktop-{VERSION}/'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as out:
        out.write(JAR, prefix + JAR.name)
        for name in ('run.sh', 'run.bat', 'README.md', 'RELEASE_NOTES.md'):
            out.write(ROOT / name, prefix + name)
        # Preserve the original dependency distributions and downloaded source
        # archives so their upstream licensing information accompanies the binary.
        dependencies = sorted((ROOT / 'target/dependency').glob('*.jar')) + sorted((ROOT / 'agent/target/dependency').glob('*.jar'))
        if not dependencies:
            raise SystemExit('Runtime dependency distributions missing')
        for dependency in dependencies:
            out.write(dependency, prefix + 'third-party/' + dependency.name)
            with zipfile.ZipFile(dependency) as dep:
                for member in dep.namelist():
                    if member.endswith('pom.xml'):
                        out.writestr(prefix + 'third-party/metadata/' + dependency.stem + '-pom.xml', dep.read(member))
        maven = Path(os.environ.get('MAVEN_REPO', str(Path.home() / '.m2/repository')))
        for source in sorted((maven / 'com/googlecode/soundlibs').glob('**/*-sources.jar')) + sorted((maven / 'org/ow2/asm').glob('**/*-sources.jar')):
            out.write(source, prefix + 'third-party/sources/' + source.name)
    # Also publish the standalone shaded executable, for users who already have Java.
    standalone = output / JAR.name
    standalone.write_bytes(JAR.read_bytes())
    lines = []
    for path in (standalone, archive):
        lines.append(hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + path.name)
    (output / 'SHA256SUMS.txt').write_text('\n'.join(lines) + '\n')
    print('\n'.join(lines))


if __name__ == '__main__':
    verify() if '--verify' in sys.argv else package()
