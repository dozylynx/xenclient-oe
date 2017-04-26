require recipes-extended/xen/xen.inc
require xen-common.inc

DESCRIPTION = "Xen hypervisor, 64-bit build"

# In OpenXT, multiple recipes are used to build Xen and its components:
# a 32-bit build of tools ; a 64-bit hypervisor; and a separate blktap
# build to fix potentially circular dependencies with libv4v and icbinn.
#
# This recipe shares a common xen.inc with other recipes.
# PN in this recipe is "xen-hypervisor", rather than "xen" as xen.inc is
# written to expect, so in order to produce the expected package names
# with a "xen-" rather than "xen-hypervisor-" prefix, this python section
# renames the FILES_... variables defined in xen.inc.
# Some package names are defined explicitly rather than using ${PN}.

python () {
    for PKG in ['hypervisor']:
        d.renameVar("FILES_xen-hypervisor-" + PKG, "FILES_xen-" + PKG)
}

PROVIDES = "xen-hypervisor"

PACKAGES = " \
    ${PN}-dbg \
    xen-efi \
    xen-hypervisor \
    "

FILES_xen-efi = "\
    ${exec_prefix}/lib64 \
    ${exec_prefix}/lib64/xen* \
    "

PROVIDES_xen-efi = "xen-efi"
PROVIDES_xen-hypervisor = "xen-hypervisor"

INSANE_SKIP_${PN}-dbg = "arch"

INITSCRIPT_PACKAGES = ""
SYSTEMD_PACKAGES = ""

# Undo some of the upstream xen.inc configuration to retain
# the classic OpenXT Xen build configuration:

EXTRA_OECONF_remove = " \
    --exec-prefix=/usr \
    --prefix=/usr \
    --host=${HOST_SYS} \
    --with-systemd=${systemd_unitdir}/system \
    --with-systemd-modules-load=${systemd_unitdir}/modules-load.d \
    --disable-stubdom \
    --disable-ioemu-stubdom \
    --disable-pv-grub \
    --disable-xenstore-stubdom \
    --disable-rombios \
    --disable-ocamltools \
    --with-initddir=${INIT_D_DIR} \
    --with-sysconfig-leaf-dir=default \
    --with-system-qemu=/usr/bin/qemu-system-i386 \
    --disable-qemu-traditional \
"

EXTRA_OEMAKE += " \
    XEN_TARGET_ARCH=x86_64 \
    XEN_VENDORVERSION=-xc \
    "

do_configure() {
    echo "debug := n" > .config
    echo "XSM_ENABLE := y" >> .config
    echo "FLASK_ENABLE := y" >> .config

    # Enabling an IOMMU parameter locks it to its more secure setting at
    # build time, forcing the host to panic if it cannot be attained.
    # Fixing the values at build time enables code elimination of unwanted
    # logic from the hypervisor, increasing the work an adversary has to
    # perform to silently disable VT-d on a running system.

    if [ "${REQUIRE_IOMMU}" == "1" ] ; then

        # Basic values:
        echo "IOMMU_ALWAYS_ENABLED := y" >> .config
        echo "IOMMU_NEVER_PASSTHROUGH := y" >> .config
        echo "IOMMU_NEVER_DEBUG := y" >> .config
        echo "IOMMU_NEVER_WORKAROUND_BIOS_BUG := y" >> .config

        if [ "${ALLOW_INSECURE_IOMMU}" != "1" ] ; then

            # Interrupt remapping is an important security setting:
            # https://xenbits.xen.org/xsa/advisory-59.html
            # https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2011-1898
            # http://invisiblethingslab.com/resources/2011/Software%20Attacks%20on%20Intel%20VT-d.pdf

            # Queued invalidation is a requirement for interrupt remapping.
            echo "IOMMU_ALWAYS_QINVAL := y" >> .config
            echo "IOMMU_ALWAYS_INTREMAP := y" >> .config

            # The Force IOMMU flag has effects beyond just the one-off boot-time
            # check for IOMMU being enabled: it changes the logic for determining
            # which remapping engines are enabled.
            echo "IOMMU_ALWAYS_FORCE_IOMMU := y" >> .config
        fi

        # Other available IOMMU parameters beyond the above:

        # Snoop control does not appear to be available on the OptiPlex 9020 or 980.
        # echo "IOMMU_ALWAYS_SNOOP := y" >> .config

        # Setting 'dom0 strict' forces the IOMMU DMA remapping engines to be active
        # even for devices assigned to dom0. Further testing is required to
        # determine the practical effect on graphics and storage devices before
        # forcing this on.
        # echo "IOMMU_ALWAYS_DOM0_STRICT := y" >> .config
    fi
}

do_compile() {
        oe_runmake dist-xen
}

do_install() {
        install -d ${D}/boot
        oe_runmake DESTDIR=${D} install-xen
        ln -sf "`basename ${D}/boot/xen-*xc.gz`" ${D}/boot/xen-debug.gz
}

RPROVIDES_xen-efi = "xen-efi"
RPROVIDES_xen-hypervisor = "xen-hypervisor"
