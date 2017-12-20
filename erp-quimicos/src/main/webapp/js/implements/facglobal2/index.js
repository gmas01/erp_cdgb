$(function() {

    var header_root = $('#header');
    var subheader_one = header_root.find('#header1');
    var lienzo_recal = $('#lienzo_recalculable');

    var config = {
        empresa: lienzo_recal.find('input[name=emp]').val(),
        sucursal: lienzo_recal.find('input[name=suc]').val(),
        tituloApp: 'Factura Global de Sucursal',
        contextpath : lienzo_recal.find('input[name=contextpath]').val(),
        userName : lienzo_recal.find('input[name=user]').val(),
        ui: lienzo_recal.find('input[name=iu]').val(),

        getUrlForGetAndPost: function() {
            var url = document.location.protocol + '//' + document.location.host + this.getController();
            return url;
        },

        conformRestfulUrl: function( name ) {
            return this.getUrlForGetAndPost() + '/' + name + '.json';
        },

        getEmp: function() {
            return this.empresa;
        },

        getSuc: function() {
            return this.sucursal;
        },

        getUserName: function() {
            return this.userName;
        },

        getUid: function() {
            return this.ui;
        },

        getTituloApp: function() {
            return this.tituloApp;
        },

        getController: function() {
            return this.contextpath + "/controllers/facglobal2";
        }
    };

    subheader_one.find('span.emp').text( config.getEmp() );
    subheader_one.find('span.suc').text( config.getSuc() );
    subheader_one.find('span.username').text( config.getUserName() );

    //aqui va el titulo del catalogo
    $( '#barra_titulo' ).find( '#td_titulo' ).append( config.getTituloApp() );

    $( '#barra_acciones' ).hide();

    //barra para el buscador
    $( '#barra_buscador' ).hide();

    var factButton = lienzo_recal.find('input[name=facturar]');

    var goGlobalRestfulUrl = config.conformRestfulUrl( 'goGlobal' );
    var goGlobalCallback = function( entry ) {

        if ( parseInt( entry['errorCode'] ) != 0 ) {
            jAlert( entry['msg'], 'Problema en Backend !', function(r) { factButton.focus(); } );
            return false;
        }

        jAlert( entry['msg'], 'Operacion exitosa !', function(r) { factButton.focus(); } );
        return true;
    };

    factButton.click( function( event ) {
        event.preventDefault();
        $.post( goGlobalRestfulUrl, { uid: config.getUid() }, goGlobalCallback );
    });

});
