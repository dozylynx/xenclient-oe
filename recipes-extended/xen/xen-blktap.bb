require recipes-extended/xen/xen.inc
require recipes-extended/xen/xen-blktap.inc
require xen-common.inc

DESCRIPTION = "Xen hypervisor blktap2 and libvhd components"

DEPENDS += "util-linux xen-tools openssl libaio libicbinn-resolved"

PACKAGES = " \
    ${BLKTAP_PACKAGES} \
    ${PN}-dev \
    ${PN}-dbg \
    "

FILES_${PN}-dev += " \
    ${includedir}/tap-ctl.h \
    ${includedir}/tapdisk-message.h \
    "

RDEPENDS_${PN}-dev = "${PN}-blktap"

do_configure() {
    do_configure_common
}

do_compile() {
    oe_runmake -C tools subdir-all-include
    oe_runmake -C tools subdir-all-blktap2
}

do_install() {
    install -d ${D}${datadir}/pkgconfig
    oe_runmake DESTDIR=${D} -C tools subdir-install-blktap2
    install -d ${D}/usr/include
    install tools/blktap2/control/tap-ctl.h ${D}/usr/include
    install tools/blktap2/include/tapdisk-message.h ${D}/usr/include

    # /usr/share is not packaged, removing to silence QA warnings
    rm -rf ${D}/${datadir}
}

RDEPENDS_${PN} += "glibc-gconv-utf-16"
RCONFLICTS_${PN} = "blktap3"
RCONFLICTS_${PN}-blktapctl = "blktap3"
RCONFLICTS_${PN}-libvhd = "blktap3"
