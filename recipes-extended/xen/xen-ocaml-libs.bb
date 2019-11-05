require recipes-extended/xen/xen.inc
require xen-common.inc

inherit ocaml findlib

DESCRIPTION = "Xen hypervisor ocaml libs and xenstore components"

SRC_URI_append = " \
    file://xenstored.initscript \
    file://oxenstored.conf \
    "

PACKAGES = " \
    ${PN}-xenstored \
    ${PN}-dev \
    ${PN}-dbg \
    ${PN}-staticdev \
    ${PN} \
    "

PROVIDES =+ "virtual/xenstored"

DEPENDS += " \
    util-linux \
    xen \
    ${@bb.utils.contains('DISTRO_FEATURES', 'blktap2', 'xen-blktap', 'blktap3', d)} \
    libnl \
    "

# OpenXT packages both the C and OCaml versions of XenStored.
# This recipe packages the OCaml daemon; xen.bb packages the C one.
FILES_${PN}-xenstored = " \
    ${sbindir}/xenstored.${PN}-xenstored \
    ${localstatedir}/lib/xenstored \
    ${sysconfdir}/init.d/xenstored.${PN}-xenstored \
    ${sysconfdir}/xen/oxenstored.conf \
    "
RPROVIDES_${PN}-xenstored = "virtual/xenstored"

EXTRA_OECONF_remove = "--disable-ocamltools"

CFLAGS_prepend += " -I${STAGING_INCDIR}/blktap "

# OCAMLDESTDIR is set to $DESTDIR/$(ocamlfind printconf destdir), yet DESTDIR
# is required for other binaries installation, so override OCAMLDESTDIR.
EXTRA_OEMAKE += " \
    CROSS_SYS_ROOT=${STAGING_DIR_HOST} \
    CROSS_COMPILE=${HOST_PREFIX} \
    CONFIG_IOEMU=n \
    DESTDIR=${D} \
    OCAMLDESTDIR=${D}${sitelibdir} \
    "

EXTRA_OECONF += " --enable-blktap2 "

TARGET_CC_ARCH += "${LDFLAGS}"
CC_FOR_OCAML="${TARGET_PREFIX}gcc"

INITSCRIPT_PACKAGES = "${PN}-xenstored"
INITSCRIPT_NAME_${PN}-xenstored = "xenstored"
INITSCRIPT_PARAMS_${PN}-xenstored = "defaults 05"

pkg_postinst_${PN}-xenstored () {
    update-alternatives --install ${sbindir}/xenstored xenstored xenstored.${PN}-xenstored 100
    update-alternatives --install ${sysconfdir}/init.d/xenstored xenstored-initscript xenstored.${PN}-xenstored 100
}

pkg_prerm_${PN}-xenstored () {
    update-alternatives --remove xenstored xenstored.${PN}-xenstored
    update-alternatives --remove xenstored-initscript xenstored.${PN}-xenstored
}

do_configure() {
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

    # ocamlopt/ocamlc -cc argument will treat everything following it as the
    # executable name, so wrap everything.
    cat - > ocaml-cc.sh <<EOF
#! /bin/sh
exec ${CC} "\$@"
EOF
    chmod +x ocaml-cc.sh

    oe_runmake V=1 \
       CC="${B}/ocaml-cc.sh" \
       LDLIBS_libxenctrl='-lxenctrl' \
       LDLIBS_libxenstore='-lxenstore' \
       LDLIBS_libblktapctl='-lblktapctl' \
       LDLIBS_libxenguest='-lxenguest' \
       LDLIBS_libxentoollog='-lxentoollog' \
       LDLIBS_libxenevtchn='-lxenevtchn' \
       -C tools subdir-all-ocaml
}

do_install() {
    oe_runmake -C tools/ocaml install

    mv ${D}/usr/sbin/oxenstored ${D}/${sbindir}/xenstored.${PN}-xenstored
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/xenstored.initscript \
                    ${D}${sysconfdir}/init.d/xenstored.${PN}-xenstored
    rm ${D}${sysconfdir}/xen/oxenstored.conf
    install -m 0644 ${WORKDIR}/oxenstored.conf \
                    ${D}${sysconfdir}/xen/oxenstored.conf
}
