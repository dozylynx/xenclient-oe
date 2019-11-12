require recipes-extended/xen/xen.inc
require xen-common.inc

DESCRIPTION = "Xen hypervisor libxl components"

inherit setuptools update-rc.d

SRC_URI_append = " \
    file://xen-init-dom0.initscript \
    file://xl.conf \
    "

DEPENDS += " \
    util-linux \
    xen \
    ${@bb.utils.contains('DISTRO_FEATURES', 'blktap2', 'xen-blktap', 'blktap3', d)} \
    libnl \
    "

# inherit setuptools adds python to RDEPENDS, override it
RDEPENDS_${PN} = ""

RDEPENDS_${PN}-xl += " \
    xen-tools-scripts-block \
"

RDEPENDS_${PN}-dev = " \
    ${PN}-libxenlight \
    ${PN}-libxlutil \
"

PACKAGES = " \
    ${PN}-xl \
    ${PN}-dev \
    ${PN}-libxlutil \
    ${PN}-libxlutil-dev \
    ${PN}-libxenlight \
    ${PN}-libxenlight-dev \
    ${PN}-staticdev \
    ${PN}-dbg \
    ${PN}-cmp-fd-file-inode \
    "

FILES_${PN}-staticdev = " \
    ${libdir}/libxlutil.a \
    ${libdir}/libxenlight.a \
    "
FILES_${PN}-libxlutil += " \
    ${sysconfdir}/xen/xl.conf \
"
FILES_${PN}-dev += " \
    ${includedir} \
"
FILES_${PN}-dbg += " \
    ${bindir}/.debug \
    ${sbindir}/.debug \
    ${libdir}/.debug \
    /usr/src/debug \
"
FILES_${PN}-cmp-fd-file-inode = " \
    ${libdir}/xen/bin/cmp-fd-file-inode \
"

FILES_${PN}-xl = "\
    ${sysconfdir}/bash_completion.d/xl.sh \
    ${sysconfdir}/xen/xl.conf \
    ${libdir}/xen/bin/libxl-save-helper \
    ${sbindir}/xl \
    ${libdir}/xen/bin/xen-init-dom0 \
    ${sysconfdir}/init.d/xen-init-dom0 \
    "

FILES_${PN}-libxenlight = "${libdir}/libxenlight.so.*"
FILES_${PN}-libxenlight-dev = " \
    ${libdir}/libxenlight.so \
    ${libdir}/pkgconfig/xenlight.pc \
    ${datadir}/pkgconfig/xenlight.pc \
    "

FILES_${PN}-libxlutil = "${libdir}/libxlutil.so.*"
FILES_${PN}-libxlutil-dev = " \
    ${libdir}/libxlutil.so \
    ${libdir}/pkgconfig/xlutil.pc \
    ${datadir}/pkgconfig/xlutil.pc \
    "

CFLAGS_prepend += "${@bb.utils.contains('DISTRO_FEATURES', 'blktap2', '', '-I${STAGING_INCDIR}/blktap',d)}"

EXTRA_OEMAKE += "CROSS_SYS_ROOT=${STAGING_DIR_HOST} CROSS_COMPILE=${HOST_PREFIX}"
EXTRA_OEMAKE += "CONFIG_IOEMU=n"
EXTRA_OEMAKE += "CONFIG_TESTS=n"
EXTRA_OEMAKE += "DESTDIR=${D}"

EXTRA_OECONF += " --enable-blktap2 --with-system-ipxe=/usr/share/firmware/82540em.rom "

#Make sure we disable all compiler optimizations to avoid a nasty segfault in the 
#reboot case.
BUILD_LDFLAGS += " -Wl,-O0 -O0"
BUILDSDK_LDFLAGS += " -Wl,-O0 -O0"
TARGET_LDFLAGS += " -Wl,-O0 -O0"
BUILD_OPTIMIZATION = "-pipe"
FULL_OPTIMIZATION = "-pipe ${DEBUG_FLAGS}"

TARGET_CC_ARCH += "${LDFLAGS}"
CC_FOR_OCAML="${TARGET_PREFIX}gcc"

INITSCRIPT_PACKAGES = "${PN}-xl"
INITSCRIPT_NAME_${PN}-xl = "xen-init-dom0"
INITSCRIPT_PARAMS_${PN}-xl = "defaults 21"

do_configure() {
	#remove optimizations in the config files
	sed -i 's/-O2//g' ${S}/Config.mk
	sed -i 's/-O2//g' ${S}/config/StdGNU.mk

	cp "${WORKDIR}/defconfig" "${B}/xen/.config"

    do_configure_common
}

do_compile() {
    oe_runmake -C tools/libs subdir-all-toolcore
    oe_runmake -C tools subdir-all-include
    oe_runmake LDLIBS_libxenctrl='-lxenctrl' \
		       LDLIBS_libxenstore='-lxenstore' \
		       LDLIBS_libblktapctl='-lblktapctl' \
		       LDLIBS_libxenguest='-lxenguest' \
		       LDLIBS_libxentoollog='-lxentoollog' \
		       LDLIBS_libxenevtchn='-lxenevtchn' \
		       -C tools subdir-all-libxl
    oe_runmake LDLIBS_libxenctrl='-lxenctrl' \
		       LDLIBS_libxenstore='-lxenstore' \
		       LDLIBS_libblktapctl='-lblktapctl' \
		       LDLIBS_libxenguest='-lxenguest' \
		       LDLIBS_libxentoollog='-lxentoollog' \
		       LDLIBS_libxenevtchn='-lxenevtchn' \
		       -C tools subdir-all-xl
    oe_runmake LDLIBS_libxenctrl='-lxenctrl' \
		       LDLIBS_libxenstore='-lxenstore' \
		       LDLIBS_libblktapctl='-lblktapctl' \
		       LDLIBS_libxenguest='-lxenguest' \
		       LDLIBS_libxentoollog='-lxentoollog' \
		       LDLIBS_libxenevtchn='-lxenevtchn' \
		       -C tools subdir-all-helpers
}

do_install() {
    install -d ${D}${datadir}/pkgconfig
    oe_runmake DESTDIR=${D} -C tools subdir-install-libxl
    oe_runmake DESTDIR=${D} -C tools subdir-install-xl
    oe_runmake DESTDIR=${D} -C tools subdir-install-helpers
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/xen-init-dom0.initscript \
                    ${D}${sysconfdir}/init.d/xen-init-dom0
    install -d ${D}${sysconfdir}/xen
    install -m 0644 ${WORKDIR}/xl.conf \
                    ${D}${sysconfdir}/xen/xl.conf

    # Since we don't have a xenstore stubdomain, remove the
    # xenstore stubdomain init program (libdir == /usr/lib)
    rm -f ${D}/${libdir}/xen/bin/init-xenstore-domain
}
