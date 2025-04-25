import React from 'react';
import { Link } from 'react-router-dom';
// Optionally install date-fns for custom formats:
// import { format } from 'date-fns';

export default function TableComponent({
                                           tableLabelPlural = '',
                                           tableLabel = '',
                                           tableName = '',
                                           fieldsToRender = [],
                                           allFields = [],
                                           columnLabels = {},
                                           records = [],
                                           contextPath = '',
                                           loadContent,
                                           deleteRecord,
                                           /**
                                            * Optional map of field name to formatting function:
                                            * e.g. { created_at: ts => new Date(ts).toLocaleString() }
                                            */
                                           cellFormatters = {},
                                       }) {
    // Fallback if fieldsToRender is empty
    const renderFields =
        fieldsToRender && fieldsToRender.length > 0 ? fieldsToRender : allFields;

    const formatCell = (field, value) => {
        const formatter = cellFormatters[field];
        if (formatter) {
            // Guard against empty or invalid values
            if (value == null || value === '' || Number.isNaN(Number(value))) {
                return '';
            }
            return formatter(value);
        }
        // No formatter: display raw value (or empty string if null/undefined)
        return value == null ? '' : String(value);
    };

    return (
        <div>
            <h2>{tableLabelPlural}</h2>
            <Link to={`/table/${tableName}/create`} className="btn btn-primary">
                Create New {tableLabel}
            </Link>
            <br />
            <br />
            <table className="user-table">
                <thead>
                <tr>
                    {renderFields.map((field, index) => (
                        <th key={index}>{columnLabels[field]}</th>
                    ))}
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {records.map((record) => (
                    <tr key={record.id}>
                        {renderFields.map((field, i) => (
                            <td key={i}>{formatCell(field, record[field])}</td>
                        ))}
                        <td>
                            <Link
                                to={`/table/${tableName}/${record.id}/edit`}
                                className="btn btn-secondary"
                            >
                                Edit
                            </Link>
                            <a
                                href="#"
                                className="btn btn-danger"
                                onClick={(e) => {
                                    e.preventDefault();
                                    deleteRecord(tableName, record.id);
                                }}
                            >
                                Delete
                            </a>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}
