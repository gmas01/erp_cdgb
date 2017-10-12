CREATE OR REPLACE FUNCTION fac_save_xml(
    file_xml character varying,
    prefact_id integer,
    usr_id integer,
    creation_date character varying,
    no_id_emp character varying,
    serie character varying,
    _folio character varying,
    items_str character varying,
    traslados_str character varying,
    retenciones_str character varying,
    reg_fiscal character varying,
    pay_method character varying,
    exp_place character varying,
    purpose character varying,
    no_aprob character varying,
    ano_aprob character varying,
    rfc_custm character varying,
    rs_custm character varying,
    account_number character varying,
    total_tras double precision,
    subtotal_with_desc double precision,
    total double precision,
    refact boolean
) RETURNS character varying AS
$BODY$

    -- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    -- >> Save xml data in DB          >>
    -- >> Version: CDGB                >>
    -- >> Date: 20/Jul/2017            >>
    -- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

DECLARE
    str_filas text[];
    --Total de elementos de arreglo
    total_filas integer;
    --Contador de filas o posiciones del arreglo
    cont_fila integer;

    valor_retorno character varying = '';
    ultimo_id integer:=0;
    ultimo_id_det integer:=0;
    id_tipo_consecutivo integer=0;
    prefijo_consecutivo character varying = '';
    nuevo_consecutivo bigint=0;
    nuevo_folio character varying = '';
    ultimo_id_proceso integer =0;
    tipo_de_documento integer =0;
    fila_fac_rem_doc record;

    app_selected integer;

    emp_id integer:=0;
    suc_id integer:=0;
    suc_id_consecutivo integer:=0; --sucursal de donde se tomara el consecutivo
    id_almacen integer;
    espacio_tiempo_ejecucion timestamp with time zone = now();
    ano_actual integer:=0;
    mes_actual integer:=0;
    factura_fila record;
    prefactura_fila record;
    prefactura_detalle record;
    factura_detalle record;
    tiene_pagos integer:=0;
    identificador_nuevo_movimiento integer;
    tipo_movimiento_id integer:=0;
    exis integer:=0;
    sql_insert text;
    sql_update text;
    sql_select text;
    sql_select2 character varying:='';
    cantidad_porcentaje double precision:=0;
    id_proceso integer;
    bandera_tipo_4 boolean;--bandera que identifica si el producto es tipo 4, true=tipo 4, false=No es tipo4
    serie_folio_fac character varying:='';
    tipo_cam double precision := 0;

    numero_dias_credito integer:=0;
    fecha_de_vencimiento timestamp with time zone;

    importe_del_descto_partida double precision := 0;
    importe_partida_con_descto double precision := 0;
    suma_descuento double precision := 0;
    suma_subtotal_con_descuento double precision := 0;

    importe_partida double precision := 0;
    importe_ieps_partida double precision := 0;
    impuesto_partida double precision := 0;
    monto_subtotal double precision := 0;
    suma_ieps double precision := 0;
    suma_total double precision := 0;
    monto_impuesto double precision := 0;
    total_retencion double precision := 0;
    retener_iva boolean := false;
    tasa_retencion double precision := 0;
    retencion_partida double precision := 0;
    suma_retencion_de_partidas double precision := 0;
    suma_retencion_de_partidas_globlal double precision:= 0;

    --Estas variables se utilizan en caso de que se facture un pedido en otra moneda
    suma_descuento_global double precision := 0;
    suma_subtotal_con_descuento_global double precision := 0;
    monto_subtotal_global double precision := 0;
    suma_ieps_global double precision := 0;
    monto_impuesto_global double precision := 0;
    total_retencion_global double precision := 0;
    suma_total_global double precision := 0;
    cant_original double precision := 0;

    total_factura double precision;
    id_moneda_factura integer:=0;
    suma_pagos double precision:=0;

    costo_promedio_actual double precision:=0;
    costo_referencia_actual double precision:=0;

    id_osal integer := 0;
    fila record;
    fila_detalle record;
    facpar record;--parametros de Facturacion

    id_df integer:=0;--id de la direccion fiscal
    result character varying:='';

    noDecUnidad integer:=0;--numero de decimales permitidos para la unidad
    exisActualPres double precision:=0;--existencia actual de la presentacion
    equivalenciaPres double precision:=0; --equivalencia de la presentacion en la unidad del producto
    cantPres double precision:=0; --Cantidad que se esta Intentando traspasar
    cantPresAsignado double precision:=0;
    cantPresReservAnterior double precision:=0;

    controlExisPres boolean; --Variable que indica  si se debe controlar Existencias por Presentacion
    partida_facturada boolean;--Variable que indica si la cantidad de la partida ya fue facturada en su totalidad
    actualizar_proceso boolean; --Indica si hay que actualizar el flujo del proceso. El proceso se debe actualizar cuando ya no quede partidas vivas
    id_pedido integer;--Id del Pedido que se esta facturando
    --Id de la unidad de medida del producto
    idUnidadMedida integer:=0;
    --Nombre de la unidad de medida del producto
    nombreUnidadMedida character varying:=0;
    --Densidad del producto
    densidadProd double precision:=0;
    --Cantidad en la unidad del producto
    cantUnidadProd double precision:=0;
    --Id de la unidad de Medida de la Venta
    idUnidadMedidaVenta integer:=0;
    --Cantidad en la unidad de Venta, esto se utiliza cuando la unidad del producto es diferente a la de venta
    cantUnidadVenta double precision:=0;
    --Cantidad de la existencia convertida a la unidad de venta, esto se utiliza cuando la unidad del producto es diferente a la de venta
    cantExisUnidadVenta double precision:=0;
    match_cadena boolean:=false;

BEGIN

    app_selected := 13;
	
    SELECT EXTRACT(YEAR FROM espacio_tiempo_ejecucion) INTO ano_actual;
    SELECT EXTRACT(MONTH FROM espacio_tiempo_ejecucion) INTO mes_actual;
	
    --obtener id de empresa, sucursal
    SELECT gral_suc.empresa_id, gral_usr_suc.gral_suc_id
    FROM gral_usr_suc 
    JOIN gral_suc ON gral_suc.id = gral_usr_suc.gral_suc_id
    WHERE gral_usr_suc.gral_usr_id = usr_id
    INTO emp_id, suc_id;
	
    --Obtener parametros para la facturacion
    SELECT * FROM fac_par WHERE gral_suc_id=suc_id INTO facpar;
	
    --tomar el id del almacen para ventas
    id_almacen := facpar.inv_alm_id;
	
    --éste consecutivo es para el folio de Remisión y folio para BackOrder(poc_ped_bo)
    suc_id_consecutivo := facpar.gral_suc_id_consecutivo;

	--query para verificar si la Empresa actual incluye Modulo de Produccion y control de Existencias por Presentacion
    SELECT control_exis_pres FROM gral_emp WHERE id=emp_id INTO controlExisPres;
	
    --Inicializar en cero
    id_pedido:=0;
			
    tipo_de_documento := 1; --Factura
			
    serie_folio_fac:= serie||_folio;
			
    --extraer datos de la Prefactura
    SELECT * FROM erp_prefacturas WHERE id=prefact_id INTO prefactura_fila;
			
    --Obtener el numero de dias de credito
    SELECT dias FROM cxc_clie_credias WHERE id=prefactura_fila.terminos_id INTO numero_dias_credito;
			
    --calcula la fecha de vencimiento a partir de la fecha de la factura
    SELECT (to_char(espacio_tiempo_ejecucion,'yyyy-mm-dd')::DATE + numero_dias_credito)::timestamp with time zone AS fecha_vencimiento INTO fecha_de_vencimiento;
			
    IF prefactura_fila.moneda_id=1 THEN 
        tipo_cam:=1;
    ELSE
        tipo_cam:=prefactura_fila.tipo_cambio;
    END IF;
			
    --Toma la fecha de la Facturación. Ésta fecha es la misma que se le asigno al xml
    espacio_tiempo_ejecucion := translate(creation_date,'T',' ')::timestamp with time zone;
			
    --crea registro en fac_cfds
    INSERT INTO fac_cfds(
        rfc_cliente,--rfc_custm,
        serie,--serie,
        folio_del_comprobante_fiscal,--folio,
        numero_de_aprobacion,--no_aprob,
        monto_de_la_operacion,--total,
        monto_del_impuesto,--total_tras,
        estado_del_comprobante,--'1',
        nombre_archivo,--file_xml,
        momento_expedicion,--creation_date,
        razon_social,--rs_custm,
        tipo_comprobante,--'I',
        proposito,--purpose,
        anoaprovacion, --ano_aprob,
        serie_folio, --serie_folio_fac,
        conceptos, --items_str,
        impuestos_trasladados, --traslados_str,
        impuestos_retenidos, --retenciones_str,
        regimen_fiscal, --reg_fiscal,
        metodo_pago, --pay_method,
        numero_cuenta, --account_number,
        lugar_expedicion,--exp_place,
        tipo_de_cambio,--tipo_cam,
        gral_mon_id,--prefactura_fila.moneda_id,
        id_user_crea,-- usr_id
        empresa_id,--emp_id,
        sucursal_id,--suc_id,
        proceso_id--prefactura_fila.proceso_id
    ) VALUES(rfc_custm, serie, _folio, no_aprob, total, total_tras, '1', file_xml, creation_date, rs_custm, 'I', purpose, ano_aprob, serie_folio_fac, items_str, traslados_str, retenciones_str, reg_fiscal, pay_method, account_number, exp_place, tipo_cam, prefactura_fila.moneda_id, usr_id, emp_id, suc_id, prefactura_fila.proceso_id);


    --crea registro en erp_h_facturas
    INSERT INTO erp_h_facturas(
        cliente_id,--prefactura_fila.cliente_id,
        cxc_agen_id,--prefactura_fila.empleado_id,
        serie_folio,--serie_folio_fac,
        monto_total,--prefactura_fila.fac_total,
        saldo_factura,--prefactura_fila.fac_total,
        moneda_id,--prefactura_fila.moneda_id,
        tipo_cambio,--tipo_cam,
        momento_facturacion,--espacio_tiempo_ejecucion,
        fecha_vencimiento,--fecha_de_vencimiento,
        subtotal,--prefactura_fila.fac_subtotal,
        monto_ieps, --prefactura_fila.fac_monto_ieps,
        impuesto,--prefactura_fila.fac_impuesto,
        retencion,--prefactura_fila.fac_monto_retencion,
        orden_compra,--prefactura_fila.orden_compra,
        id_usuario_creacion, --usr_id,
        empresa_id, --emp_id,
        sucursal_id--suc_id
    )VALUES(prefactura_fila.cliente_id, prefactura_fila.empleado_id, serie_folio_fac, prefactura_fila.fac_total, prefactura_fila.fac_total, prefactura_fila.moneda_id, tipo_cam, espacio_tiempo_ejecucion, fecha_de_vencimiento, prefactura_fila.fac_subtotal, prefactura_fila.fac_monto_ieps, prefactura_fila.fac_impuesto, prefactura_fila.fac_monto_retencion, prefactura_fila.orden_compra, usr_id, emp_id, suc_id);

    --Crea registros en la tabla fac_docs
    INSERT INTO fac_docs(
        serie_folio,--serie_folio_fac,
        folio_pedido,--prefactura_fila.folio_pedido,
        cxc_clie_id,--prefactura_fila.cliente_id,
        moneda_id,--prefactura_fila.moneda_id,
        subtotal,--prefactura_fila.fac_subtotal,
        monto_ieps,--prefactura_fila.fac_monto_ieps,
        impuesto,--prefactura_fila.fac_impuesto,
        monto_retencion,--prefactura_fila.fac_monto_retencion,
        total,--prefactura_fila.fac_total,
        tasa_retencion_immex,--prefactura_fila.tasa_retencion_immex,
        tipo_cambio,--tipo_cam,
        proceso_id,--prefactura_fila.proceso_id,
        cxc_agen_id,--prefactura_fila.empleado_id,
        terminos_id,--prefactura_fila.terminos_id,
        fecha_vencimiento,--fecha_de_vencimiento
        orden_compra,--prefactura_fila.orden_compra,
        observaciones, --prefactura_fila.observaciones,
        fac_metodos_pago_id, --prefactura_fila.fac_metodos_pago_id,
        no_cuenta, --prefactura_fila.no_cuenta,
        enviar_ruta,--prefactura_fila.enviar_ruta,
        inv_alm_id,--prefactura_fila.inv_alm_id
        cxc_clie_df_id,--prefactura_fila.cxc_clie_df_id,
        momento_creacion,--translate(creation_date,'T',' ')::timestamp with time zone,,
        gral_usr_id_creacion, --usr_id,
        ref_id, --no_id_emp 
        monto_descto, --prefactura_fila.fac_monto_descto
        motivo_descto, --prefactura_fila.motivo_descto,
        subtotal_sin_descto, --subtotal_with_desc 
        ctb_tmov_id --prefactura_fila.ctb_tmov_id 
    ) VALUES (serie_folio_fac, prefactura_fila.folio_pedido, prefactura_fila.cliente_id, prefactura_fila.moneda_id, prefactura_fila.fac_subtotal, prefactura_fila.fac_monto_ieps, prefactura_fila.fac_impuesto, prefactura_fila.fac_monto_retencion, prefactura_fila.fac_total, prefactura_fila.tasa_retencion_immex, tipo_cam, prefactura_fila.proceso_id, prefactura_fila.empleado_id, prefactura_fila.terminos_id, fecha_de_vencimiento, prefactura_fila.orden_compra, prefactura_fila.observaciones, prefactura_fila.fac_metodos_pago_id, prefactura_fila.no_cuenta, prefactura_fila.enviar_ruta, prefactura_fila.inv_alm_id, prefactura_fila.cxc_clie_df_id, translate(creation_date,'T',' ')::timestamp with time zone, usr_id, no_id_emp, prefactura_fila.fac_monto_descto, prefactura_fila.motivo_descto, subtotal_with_desc, prefactura_fila.ctb_tmov_id) RETURNING id INTO ultimo_id;


    --Guarda la cadena del xml timbrado
    INSERT INTO fac_cfdis(tipo, ref_id, doc, gral_emp_id, gral_suc_id, fecha_crea, gral_usr_id_crea) 
    VALUES (1,no_id_emp,'THIS FIELD IS DEPRECATED'::text,emp_id,suc_id,translate(creation_date,'T',' ')::timestamp with time zone, usr_id);


    -- bandera que identifica si el producto es tipo 4
    -- si es tipo 4 no debe existir movimientos en inventario
    bandera_tipo_4=TRUE;
    tipo_movimiento_id:=5;--Salida por Venta
    id_tipo_consecutivo:=21; --Folio Orden de Salida
    id_almacen := prefactura_fila.inv_alm_id;--almacen de donde se hara la salida

    -- Bandera que indica si se debe actualizar el flujo del proceso.
    -- El proceso solo debe actualizarse cuando no quede ni una sola partida viva
    actualizar_proceso:=true;

    -- refact=false:No es refacturacion
    -- tipo_documento=1:Factura
    IF refact IS NOT true AND prefactura_fila.tipo_documento=1 THEN
        -- aqui entra para tomar el consecutivo del folio  la sucursal actual
        UPDATE gral_cons SET consecutivo=( SELECT sbt.consecutivo + 1  FROM gral_cons AS sbt WHERE sbt.id=gral_cons.id )
        WHERE gral_emp_id=emp_id AND gral_suc_id=suc_id AND gral_cons_tipo_id=id_tipo_consecutivo  RETURNING prefijo,consecutivo INTO prefijo_consecutivo,nuevo_consecutivo;

        -- concatenamos el prefijo y el nuevo consecutivo para obtener el nuevo folio 
        nuevo_folio := prefijo_consecutivo || nuevo_consecutivo::character varying;
				
        -- genera registro en tabla inv_osal(Orden de Salida)
        INSERT INTO inv_osal(folio,estatus,erp_proceso_id,inv_mov_tipo_id,tipo_documento,folio_documento,fecha_exp,gral_app_id,cxc_clie_id,inv_alm_id,subtotal,monto_iva,monto_retencion,monto_total,folio_pedido,orden_compra,moneda_id,tipo_cambio,momento_creacion,gral_usr_id_creacion, gral_emp_id, gral_suc_id, monto_ieps)
        VALUES(nuevo_folio,0,prefactura_fila.proceso_id,tipo_movimiento_id,tipo_de_documento,serie_folio_fac,espacio_tiempo_ejecucion,app_selected,prefactura_fila.cliente_id,id_almacen, prefactura_fila.fac_subtotal, prefactura_fila.fac_impuesto, prefactura_fila.fac_monto_retencion, prefactura_fila.fac_total, prefactura_fila.folio_pedido,prefactura_fila.orden_compra,prefactura_fila.moneda_id,tipo_cam,espacio_tiempo_ejecucion,usr_id, emp_id, suc_id, prefactura_fila.fac_monto_ieps) RETURNING id INTO id_osal;
				
        -- genera registro del movimiento
        INSERT INTO inv_mov(observacion,momento_creacion,gral_usr_id, gral_app_id,inv_mov_tipo_id, referencia, fecha_mov )
        VALUES (prefactura_fila.observaciones,espacio_tiempo_ejecucion,usr_id,app_selected, tipo_movimiento_id, serie_folio_fac, translate(creation_date,'T',' ')::timestamp with time zone) RETURNING id INTO identificador_nuevo_movimiento;

    END IF;

    -- obtiene lista de productos de la prefactura
    sql_select:='';
    sql_select := 'SELECT  erp_prefacturas_detalles.id AS id_det,
        erp_prefacturas_detalles.producto_id,
        erp_prefacturas_detalles.presentacion_id,
        erp_prefacturas_detalles.cantidad AS cant_pedido,
        erp_prefacturas_detalles.cant_facturado,
        erp_prefacturas_detalles.cant_facturar AS cantidad,
        erp_prefacturas_detalles.tipo_impuesto_id,
        erp_prefacturas_detalles.valor_imp,
        erp_prefacturas_detalles.precio_unitario,
        inv_prod.tipo_de_producto_id as tipo_producto,
        erp_prefacturas_detalles.costo_promedio,
        erp_prefacturas_detalles.costo_referencia, 
        erp_prefacturas_detalles.reservado,
        erp_prefacturas_detalles.reservado AS nuevo_reservado,
        (CASE WHEN inv_prod_presentaciones.id IS NULL THEN 0 ELSE inv_prod_presentaciones.cantidad END) AS cant_equiv,
        (CASE WHEN inv_prod_unidades.id IS NULL THEN 0 ELSE inv_prod_unidades.decimales END) AS no_dec,
        inv_prod.unidad_id AS id_uni_prod,
        inv_prod.densidad AS densidad_prod,
        inv_prod_unidades.titulo AS nombre_unidad,
        erp_prefacturas_detalles.inv_prod_unidad_id,
        erp_prefacturas_detalles.gral_ieps_id,
        erp_prefacturas_detalles.valor_ieps,
        (CASE WHEN erp_prefacturas_detalles.descto IS NULL THEN 0 ELSE erp_prefacturas_detalles.descto END) AS descto,
        (CASE WHEN erp_prefacturas_detalles.fac_rem_det_id IS NULL THEN 0 ELSE erp_prefacturas_detalles.fac_rem_det_id END) AS fac_rem_det_id,
        erp_prefacturas_detalles.gral_imptos_ret_id,
        erp_prefacturas_detalles.tasa_ret  
        FROM erp_prefacturas_detalles 
        JOIN inv_prod ON inv_prod.id=erp_prefacturas_detalles.producto_id
        LEFT JOIN inv_prod_unidades ON inv_prod_unidades.id=inv_prod.unidad_id
        LEFT JOIN inv_prod_presentaciones ON inv_prod_presentaciones.id=erp_prefacturas_detalles.presentacion_id 
        WHERE erp_prefacturas_detalles.cant_facturar>0 
        AND erp_prefacturas_detalles.prefacturas_id='||prefact_id||';';

    FOR prefactura_detalle IN EXECUTE(sql_select) LOOP
        -- Inicializar valores
        cantPresReservAnterior:=0;
        cantPresAsignado:=0;
        partida_facturada:=false;

        -- tipo_documento 3=Factura de remision
        IF prefactura_fila.tipo_documento::integer = 3 THEN 
            -- toma el costo promedio que viene de la prefactura
            costo_promedio_actual := prefactura_detalle.costo_promedio;
            costo_referencia_actual := prefactura_detalle.costo_referencia;
        ELSE
            -- Obtener costo promedio actual del producto. El costo promedio es en MN.
            SELECT * FROM inv_obtiene_costo_promedio_actual(prefactura_detalle.producto_id, espacio_tiempo_ejecucion) INTO costo_promedio_actual;

            -- Obtener el costo ultimo actual del producto. Este costo es convertido a pesos
            sql_select2 := 'SELECT (CASE WHEN gral_mon_id_'||mes_actual||'=1 THEN costo_ultimo_'||mes_actual||'  ELSE costo_ultimo_'||mes_actual||' * (CASE WHEN gral_mon_id_'||mes_actual||'=1 THEN 1 ELSE tipo_cambio_'||mes_actual||' END) END) AS costo_ultimo FROM inv_prod_cost_prom WHERE inv_prod_id='||prefactura_detalle.producto_id||' AND ano='||ano_actual||';';
            EXECUTE sql_select2 INTO costo_referencia_actual;
        END IF;
				
        -- Verificar que no tenga valor null
        IF costo_promedio_actual IS NULL OR costo_promedio_actual<=0 THEN costo_promedio_actual:=0; END IF;
        IF costo_referencia_actual IS NULL OR costo_referencia_actual<=0 THEN costo_referencia_actual:=0; END IF;

        cantUnidadProd:=0;
        idUnidadMedida:=prefactura_detalle.id_uni_prod;
        densidadProd:=prefactura_detalle.densidad_prod;
        nombreUnidadMedida:=prefactura_detalle.nombre_unidad;

        IF densidadProd IS NULL OR densidadProd=0 THEN densidadProd:=1; END IF;

        cantUnidadProd := prefactura_detalle.cantidad::double precision;

        IF facpar.cambiar_unidad_medida THEN
            IF idUnidadMedida::integer<>prefactura_detalle.inv_prod_unidad_id THEN
                EXECUTE 'select '''||nombreUnidadMedida||''' ~* ''KILO*'';' INTO match_cadena;

                IF match_cadena=true THEN
                    -- Convertir a kilos
                    cantUnidadProd := cantUnidadProd::double precision * densidadProd;
                ELSE
                    EXECUTE 'select '''||nombreUnidadMedida||''' ~* ''LITRO*'';' INTO match_cadena;
                    IF match_cadena=true THEN 
                        -- Convertir a Litros
                        cantUnidadProd := cantUnidadProd::double precision / densidadProd;
                    END IF;
                END IF;

            END IF;
        END IF;

        -- Redondear cantidades
        prefactura_detalle.cant_pedido := round(prefactura_detalle.cant_pedido::numeric,prefactura_detalle.no_dec)::double precision;
        prefactura_detalle.cant_facturado := round(prefactura_detalle.cant_facturado::numeric,prefactura_detalle.no_dec)::double precision;
        prefactura_detalle.cantidad := round(prefactura_detalle.cantidad::numeric,prefactura_detalle.no_dec)::double precision;
        prefactura_detalle.reservado := round(prefactura_detalle.reservado::numeric,prefactura_detalle.no_dec)::double precision;
        prefactura_detalle.nuevo_reservado := round(prefactura_detalle.nuevo_reservado::numeric,prefactura_detalle.no_dec)::double precision;

        IF (cantUnidadProd::double precision <= prefactura_detalle.reservado::double precision) THEN
            -- Asignar la cantidad para descontar de reservado
            prefactura_detalle.reservado := cantUnidadProd::double precision;
        END IF;

        -- Calcular la nueva cantidad reservada
        prefactura_detalle.nuevo_reservado := prefactura_detalle.nuevo_reservado::double precision - prefactura_detalle.reservado::double precision;

        -- Redondaer la nueva cantidad reservada
        prefactura_detalle.nuevo_reservado := round(prefactura_detalle.nuevo_reservado::numeric,prefactura_detalle.no_dec)::double precision;

        -- crea registro en fac_docs_detalles
        INSERT INTO fac_docs_detalles(fac_doc_id,inv_prod_id,inv_prod_presentacion_id,gral_imptos_id,valor_imp,cantidad,precio_unitario,costo_promedio, costo_referencia, inv_prod_unidad_id, gral_ieps_id, valor_ieps, descto, gral_imptos_ret_id, tasa_ret) 
        VALUES (ultimo_id,prefactura_detalle.producto_id,prefactura_detalle.presentacion_id,prefactura_detalle.tipo_impuesto_id,prefactura_detalle.valor_imp,prefactura_detalle.cantidad,prefactura_detalle.precio_unitario, costo_promedio_actual, costo_referencia_actual, prefactura_detalle.inv_prod_unidad_id, prefactura_detalle.gral_ieps_id, prefactura_detalle.valor_ieps, prefactura_detalle.descto, prefactura_detalle.gral_imptos_ret_id, prefactura_detalle.tasa_ret) RETURNING id INTO ultimo_id_det;

        IF refact IS NOT true  AND prefactura_fila.tipo_documento::integer=1 THEN
            -- Si el tipo de producto es diferente de 4 el hay que descontar existencias y generar Movimientos
            -- tipo=4 Servicios
            -- para el tipo servicios NO debe generar movimientos NI descontar existencias
            IF prefactura_detalle.tipo_producto::integer<>4 THEN

                bandera_tipo_4=FALSE; -- indica que por lo menos un producto es diferente de tipo4, por lo tanto debe generarse movimientos

                -- tipo=1 Normal o Terminado
                -- tipo=2 Subensable o Formulacion o Intermedio
                -- tipo=5 Refacciones
                -- tipo=6 Accesorios
                -- tipo=7 Materia Prima
                -- tipo=8 Prod. en Desarrollo

                -- tipo=3 Kit
                -- tipo=4 Servicios
                -- IF prefactura_detalle.tipo_producto=1 OR prefactura_detalle.tipo_producto=2 OR prefactura_detalle.tipo_producto=5 OR prefactura_detalle.tipo_producto=6 OR prefactura_detalle.tipo_producto=7 OR prefactura_detalle.tipo_producto=8 THEN
                IF prefactura_detalle.tipo_producto::integer<>3 AND  prefactura_detalle.tipo_producto::integer<>4 THEN
                    -- Genera registro en detalles del movimiento
                    INSERT INTO inv_mov_detalle(producto_id, alm_origen_id, alm_destino_id, cantidad, inv_mov_id, costo, inv_prod_presentacion_id)
                    VALUES (prefactura_detalle.producto_id, id_almacen,0, cantUnidadProd, identificador_nuevo_movimiento, costo_promedio_actual, prefactura_detalle.presentacion_id);

                    -- Query para descontar producto de existencias y descontar existencia reservada porque ya se Facturó
                    sql_update := 'UPDATE inv_exi SET salidas_'||mes_actual||'=(salidas_'||mes_actual||'::double precision + '||cantUnidadProd||'::double precision), 
                        reservado=(reservado::double precision - '||prefactura_detalle.reservado||'::double precision), momento_salida_'||mes_actual||'='''||espacio_tiempo_ejecucion||'''
                        WHERE inv_alm_id='||id_almacen||'::integer AND inv_prod_id='||prefactura_detalle.producto_id||'::integer AND ano='||ano_actual||'::integer;';
                        EXECUTE sql_update;

                    IF FOUND THEN
	                -- RAISE EXCEPTION '%','FOUND'||FOUND;
                    ELSE
                        RAISE EXCEPTION '%','NOT FOUND:'||FOUND||'  No se pudo actualizar inv_exi';
                    END IF;

                    -- Crear registro en orden salida detalle
                    -- La cantidad se almacena en la unidad de venta
                    INSERT INTO inv_osal_detalle(inv_osal_id,inv_prod_id,inv_prod_presentacion_id,cantidad,precio_unitario, inv_prod_unidad_id, gral_ieps_id, valor_ieps)
                    VALUES (id_osal,prefactura_detalle.producto_id,prefactura_detalle.presentacion_id,prefactura_detalle.cantidad,prefactura_detalle.precio_unitario, prefactura_detalle.inv_prod_unidad_id, prefactura_detalle.gral_ieps_id, prefactura_detalle.valor_ieps);

                    -- Verificar si se está llevando el control de existencias por Presentaciones
                    IF controlExisPres=true THEN 
                        -- Si la configuracion indica que se validan Presentaciones desde el Pedido,entonces significa que hay reservados, por lo tanto hay que descontarlos
                        IF facpar.validar_pres_pedido=true THEN 
                            -- Convertir la cantidad reservada a su equivalente en presentaciones
                            cantPresReservAnterior := prefactura_detalle.reservado::double precision / prefactura_detalle.cant_equiv::double precision;

                            -- redondear la Cantidad de la Presentacion reservada Anteriormente
                            cantPresReservAnterior := round(cantPresReservAnterior::numeric,prefactura_detalle.no_dec)::double precision; 
                        END IF;

                        -- Convertir la cantidad de la partida a su equivalente a presentaciones
                        cantPresAsignado := cantUnidadProd::double precision / prefactura_detalle.cant_equiv::double precision;

                        -- Redondear la cantidad de Presentaciones asignado en la partida
                        cantPresAsignado := round(cantPresAsignado::numeric,prefactura_detalle.no_dec)::double precision;

                        -- Sumar salidas de inv_exi_pres
                        UPDATE inv_exi_pres SET 
                            salidas=(salidas::double precision + cantPresAsignado::double precision), reservado=(reservado::double precision - cantPresReservAnterior::double precision), 
                            momento_actualizacion=translate(creation_date,'T',' ')::timestamp with time zone, gral_usr_id_actualizacion=usr_id 
                        WHERE inv_alm_id=id_almacen AND inv_prod_id=prefactura_detalle.producto_id AND inv_prod_presentacion_id=prefactura_detalle.presentacion_id;
                        -- Termina sumar salidas
                    END IF;


                    -- :::::: Aqui inicia calculos para el control de facturacion por partida  ::::::

                    -- Calcular la cantidad facturada
                    prefactura_detalle.cant_facturado:=prefactura_detalle.cant_facturado::double precision + prefactura_detalle.cantidad::double precision;

                    -- Redondear la cantidad facturada
                    prefactura_detalle.cant_facturado := round(prefactura_detalle.cant_facturado::numeric,prefactura_detalle.no_dec)::double precision;

                    IF prefactura_detalle.cant_pedido <= prefactura_detalle.cant_facturado THEN 
                        partida_facturada:=true;
                    ELSE
                        -- Si entro aqui quiere decir que por lo menos una partida esta quedando pendiente de facturar por completo.
                        actualizar_proceso:=false;
                    END IF;

                    -- Actualizar el registro de la partida
                    UPDATE erp_prefacturas_detalles SET cant_facturado=prefactura_detalle.cant_facturado, facturado=partida_facturada, cant_facturar=0, reservado=prefactura_detalle.nuevo_reservado 
                    WHERE id=prefactura_detalle.id_det;

                    -- Obtener el id del pedido que se esta facturando
                    SELECT id FROM poc_pedidos WHERE _folio=prefactura_fila.folio_pedido ORDER BY id DESC LIMIT 1 INTO id_pedido;

                    IF id_pedido IS NULL THEN id_pedido:=0; END IF;

                    IF id_pedido<>0 THEN 
                        -- Actualizar el registro detalle del Pedido
                        UPDATE poc_pedidos_detalle SET reservado=prefactura_detalle.nuevo_reservado 
                        WHERE poc_pedido_id=id_pedido AND inv_prod_id=prefactura_detalle.producto_id AND presentacion_id=prefactura_detalle.presentacion_id;
                    END IF;

                END IF; -- termina tipo producto 1, 2, 7

            ELSE
                IF prefactura_detalle.tipo_producto::integer=4 THEN
                    -- :::::::::: Aqui inica calculos para el control de facturacion por partida ::::::::

                    -- Calcular la cantidad facturada
                    prefactura_detalle.cant_facturado:=prefactura_detalle.cant_facturado::double precision + prefactura_detalle.cantidad::double precision;

                    -- Redondear la cantidad facturada
                    prefactura_detalle.cant_facturado := round(prefactura_detalle.cant_facturado::numeric,prefactura_detalle.no_dec)::double precision;

                    IF prefactura_detalle.cant_pedido <= prefactura_detalle.cant_facturado THEN 
                        partida_facturada:=true;
                    END IF;

                    -- Actualizar el registro de la partida
                    UPDATE erp_prefacturas_detalles SET 
                        cant_facturado=prefactura_detalle.cant_facturado, 
                        facturado=partida_facturada, 
                        cant_facturar=0 
                    WHERE id=prefactura_detalle.id_det;

                END IF;
            END IF;
            -- Termina verificacion diferente de tipo 4

        ELSE
            -- tipo_documento 3=Factura de remision
            IF prefactura_fila.tipo_documento::integer = 3 THEN 
                -- :::::::: Aqui inica calculos para el control de facturacion por partida ::::::::
                -- Calcular la cantidad facturada
                prefactura_detalle.cant_facturado:=prefactura_detalle.cant_facturado::double precision + prefactura_detalle.cantidad::double precision;

                -- Redondear la cantidad facturada
                prefactura_detalle.cant_facturado := round(prefactura_detalle.cant_facturado::numeric,prefactura_detalle.no_dec)::double precision;

                IF prefactura_detalle.cant_pedido <= prefactura_detalle.cant_facturado THEN 
                    partida_facturada:=true;
                ELSE
                    -- Si entro aqui quiere decir que por lo menos una partida esta quedando pendiente de facturar por completo.
                    actualizar_proceso:=false;
                END IF;

                -- Actualizar el registro de la partida
                UPDATE erp_prefacturas_detalles SET cant_facturado=prefactura_detalle.cant_facturado, facturado=partida_facturada, cant_facturar=0, reservado=0 
                WHERE id=prefactura_detalle.id_det;

                -- Crear registros para relacionar las partidas de la Remision con las partidas de las facturas.
                INSERT INTO fac_rem_doc_det(fac_doc_id, fac_doc_det_id,fac_rem_det_id)
                VALUES(ultimo_id, ultimo_id_det, prefactura_detalle.fac_rem_det_id);

            END IF;
        END IF; 
        -- termina if que verifica si es refacturacion

    END LOOP;
			
    -- si bandera tipo 4=true, significa el producto que se esta facturando son servicios;
    -- por lo tanto hay que eliminar el movimiento de inventario
    IF bandera_tipo_4=TRUE THEN
        -- refact=false:No es refacturacion
        -- tipo_documento=1:Factura
        IF refact IS NOT true AND prefactura_fila.tipo_documento=1 THEN
            DELETE FROM inv_mov WHERE id=identificador_nuevo_movimiento;
        END IF;
    END IF;

    IF (SELECT count(prefact_det.id) FROM erp_prefacturas_detalles AS prefact_det JOIN inv_prod ON inv_prod.id=prefact_det.producto_id WHERE prefact_det.prefacturas_id=prefact_id AND inv_prod.tipo_de_producto_id<>4 AND prefact_det.facturado=false )>=1 THEN
        actualizar_proceso:=false;
    END IF;
			
    -- Verificar si hay que actualizar el flujo del proceso
    IF actualizar_proceso THEN
        -- Actualiza el flujo del proceso a 3=Facturado
        UPDATE erp_proceso SET proceso_flujo_id=3 WHERE id=prefactura_fila.proceso_id;
    ELSE
        -- Actualiza el flujo del proceso a 7=FACTURA PARCIAL
        UPDATE erp_proceso SET proceso_flujo_id=7 WHERE id=prefactura_fila.proceso_id;
    END IF;


    -- tipo_documento 3=Factura de remision
    IF prefactura_fila.tipo_documento=3 THEN
        -- buscar numero de remision que se incluyeron en esta factura
        sql_select:='SELECT DISTINCT fac_rem_id FROM fac_rems_docs WHERE erp_proceso_id = '||prefactura_fila.proceso_id;

        FOR fila_fac_rem_doc IN EXECUTE(sql_select) LOOP
            IF (SELECT count(fac_rems_docs.id) as exis FROM fac_rems_docs JOIN erp_prefacturas ON erp_prefacturas.proceso_id = fac_rems_docs.erp_proceso_id JOIN erp_prefacturas_detalles ON erp_prefacturas_detalles.prefacturas_id = erp_prefacturas.id WHERE (erp_prefacturas_detalles.cantidad::double precision - erp_prefacturas_detalles.cant_facturado::double precision)>0 AND fac_rems_docs.fac_rem_id = fila_fac_rem_doc.fac_rem_id)<=0 THEN

                -- Asignar facturado a cada remision
                UPDATE fac_rems SET facturado=TRUE WHERE id=fila_fac_rem_doc.fac_rem_id;
            END IF;

        END LOOP;

    END IF;
			
    -- Una vez terminado el Proceso se asignan ceros a estos campos
    UPDATE erp_prefacturas SET fac_subtotal=0, fac_impuesto=0, fac_monto_retencion=0, fac_total=0, fac_monto_ieps=0, fac_monto_descto=0 
    WHERE id=prefact_id;
			
    -- Actualiza el consecutivo del folio de la factura en la tabla fac_cfds_conf_folios. La actualización es por Empresa-sucursal
    UPDATE fac_cfds_conf_folios SET folio_actual=(folio_actual+1) WHERE id=(SELECT fac_cfds_conf_folios.id FROM fac_cfds_conf JOIN fac_cfds_conf_folios ON fac_cfds_conf_folios.fac_cfds_conf_id=fac_cfds_conf.id WHERE fac_cfds_conf_folios.proposito='FAC' AND fac_cfds_conf.empresa_id=emp_id AND fac_cfds_conf.gral_suc_id=suc_id);
			
    valor_retorno := '1:'||ultimo_id;--retorna el id de fac_docs
	
    RETURN valor_retorno; 

END;$BODY$
  LANGUAGE plpgsql VOLATILE COST 100;
